/**
 * 认证服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.constant.SocialConstant;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.DesensitizeUtils;
import com.iceeboot.common.utils.PasswordUtil;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.system.LoginDTO;
import com.iceeboot.framework.entity.system.SocialDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.entity.system.UserRoleDO;
import com.iceeboot.framework.service.system.*;
import com.iceeboot.framework.vo.system.LoginVO;
import com.iceeboot.framework.vo.system.SysUserVO;
import com.yungouos.pay.entity.WxOauthInfo;
import com.yungouos.pay.entity.WxWebLoginBiz;
import com.iceeboot.common.service.pay.client.YunGouWxApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import org.springframework.web.util.UriComponentsBuilder;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    @Value("${YunGouOS.key}")
    private String key;

    @Value("${YunGouOS.mchId}")
    private String mchId;

    @Value("${YunGouOS.notifyUrl}")
    private String notifyUrl;

    @Value("${YunGouOS.frontendUrl}")
    private String frontendUrl;

    @Value("${YunGouOS.wechatCallbackUrl}")
    private String wechatCallbackUrl;

    @Value("${YunGouOS.ticketExpireSeconds}")
    private Integer ticketExpireSeconds;

    @Value("${YunGouOS.stateExpireSeconds:300}")
    private Integer stateExpireSeconds;

    @Value("${iceeboot.auth.refresh-token-expire-seconds:604800}")
    private Long refreshTokenExpireSeconds;

    @Value("${iceeboot.auth.wechat-default-role-id:3}")
    private Long wechatDefaultRoleId;

    private final ISysUserService userService;

    private final ISysUserRoleService iSysUserRoleService;

    private final ISocialService iSocialService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final TransactionTemplate transactionTemplate;

    private final YunGouWxApiClient yunGouWxApiClient;

    private static final Integer TYPE = SocialConstant.SOCIAL_TYPE_WECHAT;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom random = new SecureRandom();

    private static final long REFRESH_TOKEN_CONSUMED_TTL_SECONDS = 60L;

    /** 登录失败最大次数 */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 登录失败计数窗口（秒）*/
    private static final long LOGIN_FAIL_WINDOW_SECONDS = 900L;

    /** 账号锁定时长（秒）*/
    private static final long LOGIN_LOCK_SECONDS = 1800L;

    private static final String REFRESH_TOKEN_CONSUME_SCRIPT = "local v = redis.call('GET', KEYS[1]);"
            + " if v then redis.call('DEL', KEYS[1]); redis.call('SET', KEYS[2], '1', 'EX', ARGV[1]); return v; end;"
            + " if redis.call('EXISTS', KEYS[2]) == 1 then return 'CONSUMED'; end;"
            + " return nil;";

    // 与 RedisTemplate 的 Jackson 值序列化保持一致：token 为 JSON 字符串，用户 ID 和 TTL 为数字。
    private static final DefaultRedisScript<Long> REPLACE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local old = redis.call('GET', KEYS[1]);
            if old then
                local token = cjson.decode(old);
                redis.call('DEL', cjson.decode(ARGV[4]) .. token, cjson.decode(ARGV[5]) .. token);
            end;
            redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3]);
            redis.call('SET', KEYS[2], ARGV[2], 'EX', ARGV[3]);
            return 1;
            """, Long.class);

    private static final DefaultRedisScript<Long> REVOKE_REFRESH_TOKEN_SCRIPT = new DefaultRedisScript<>("""
            local old = redis.call('GET', KEYS[1]);
            if old then
                local token = cjson.decode(old);
                redis.call('DEL', cjson.decode(ARGV[1]) .. token, cjson.decode(ARGV[2]) .. token);
            end;
            redis.call('DEL', KEYS[1]);
            return 1;
            """, Long.class);

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        // 参数校验
        if (!StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new IceeBootException(ResultCodeEnum.BAD_REQUEST.getCode(), ResponseMessage.Auth.USERNAME_OR_PASSWORD_EMPTY);
        }

        String username = loginDTO.getUsername();

        // 检查账号是否被锁定
        String lockedKey = CacheConstants.Auth.LOGIN_LOCKED + username;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockedKey))) {
            Long ttl = redisTemplate.getExpire(lockedKey, TimeUnit.MINUTES);
            throw new IceeBootException(ResultCodeEnum.LOGIN_ERROR.getCode(),
                    "账号已被锁定，请" + (ttl != null && ttl > 0 ? ttl + "分钟" : "稍") + "后再试");
        }

        // 查询用户
        UserDO user = userService.getUserByUsername(username);
        if (user == null) {
            recordLoginFailure(username);
            throw new IceeBootException(ResultCodeEnum.USER_NOT_EXIST.getCode(), ResultCodeEnum.USER_NOT_EXIST.getMessage());
        }

        // 检查用户状态
        if (!SystemConstants.Status.ACTIVE.equals(user.getStatus())) {
            throw new IceeBootException(ResultCodeEnum.USER_DISABLED.getCode(), ResultCodeEnum.USER_DISABLED.getMessage());
        }

        // 验证密码
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            recordLoginFailure(username);
            throw new IceeBootException(ResultCodeEnum.LOGIN_ERROR.getCode(), ResultCodeEnum.LOGIN_ERROR.getMessage());
        }

        // 登录成功，清除失败计数
        clearLoginFailure(username);

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);

        // 生成token
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();
        String refreshToken = generateRefreshToken();
        storeRefreshToken(user.getId(), refreshToken);

        // 获取token信息
        long tokenTimeout = StpUtil.getTokenTimeout();

        // 获取用户信息
        SysUserVO userVO = userService.getUserById(user.getId());

        // 缓存用户角色ID列表
        List<Long> roleIds = iSysUserRoleService.getRoleIdsByUserId(user.getId());
        String userRoleCacheKey = CacheConstants.Auth.USER_ROLE + user.getId();
        redisTemplate.opsForValue().set(userRoleCacheKey, roleIds);
//
//        // 获取用户菜单和权限
//        List<Map<String, Object>> userMenus = authCacheService.getUserMenus(user.getId());

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUser(userVO);
        loginVO.setExpires(tokenTimeout);
        loginVO.setTokenType("Bearer");
//        loginVO.setMenus(userMenus);
//        loginVO.setPermissions(userPermissions);

        return loginVO;
    }

    @Override
    public void logout() {
        Long userId = TokenUtils.getCurrentUserId();
        revokeUserRefreshToken(userId);
        TokenUtils.logout();
    }



    @Override
    public SysUserVO getCurrentUser() {
        Long userId = TokenUtils.getCurrentUserId();
        return userService.getUserById(userId);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new IceeBootException(ResultCodeEnum.BAD_REQUEST.getCode(), "刷新令牌不能为空");
        }

        String consumeResult = consumeRefreshToken(refreshToken);
        if (!StringUtils.hasText(consumeResult)) {
            throw new IceeBootException(ResultCodeEnum.REFRESH_TOKEN_INVALID.getCode(), ResultCodeEnum.REFRESH_TOKEN_INVALID.getMessage());
        }
        if ("CONSUMED".equals(consumeResult)) {
            throw new IceeBootException(ResultCodeEnum.REFRESH_TOKEN_CONSUMED.getCode(), ResultCodeEnum.REFRESH_TOKEN_CONSUMED.getMessage());
        }

        Long userId;
        try {
            userId = Long.valueOf(consumeResult);
        } catch (NumberFormatException e) {
            throw new IceeBootException(ResultCodeEnum.REFRESH_TOKEN_INVALID.getCode(), ResultCodeEnum.REFRESH_TOKEN_INVALID.getMessage());
        }

        UserDO user = userService.getById(userId);
        if (user == null) {
            throw new IceeBootException(ResultCodeEnum.USER_NOT_EXIST.getCode(), ResultCodeEnum.USER_NOT_EXIST.getMessage());
        }
        if (!SystemConstants.Status.ACTIVE.equals(user.getStatus())) {
            throw new IceeBootException(ResultCodeEnum.USER_DISABLED.getCode(), ResultCodeEnum.USER_DISABLED.getMessage());
        }

        StpUtil.login(userId);
        String accessToken = StpUtil.getTokenValue();
        String newRefreshToken = generateRefreshToken();
        storeRefreshToken(userId, newRefreshToken);

        SysUserVO userVO = userService.getUserById(user.getId());

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(newRefreshToken);
        loginVO.setUser(userVO);
        loginVO.setExpires(StpUtil.getTokenTimeout());
        loginVO.setTokenType("Bearer");

        return loginVO;
    }

    @Override
    public String getWechatAuthUrl() {
        log.info("微信扫码登录 - 生成授权URL");
        
        JSONObject params = new JSONObject();
        params.put("timestamp", System.currentTimeMillis());
        String state = generateState();
        params.put("state", state);
        String stateKey = CacheConstants.Auth.WECHAT_STATE + state;
        redisTemplate.opsForValue().set(stateKey, state, stateExpireSeconds.longValue(), TimeUnit.SECONDS);
        
        // 获取微信授权URL
        String type = "open-url";
        String authUrl = yunGouWxApiClient.getWxOauthUrl(mchId, buildWechatCallbackUrl(state), type, params, key);
        
        log.info("微信扫码登录 - 授权URL生成成功");
        return authUrl;
    }

    @Override
    public Object getWechatQr() {
        JSONObject params = new JSONObject();
        String state = generateState();
        params.put("state", state);
        redisTemplate.opsForValue().set(CacheConstants.Auth.WECHAT_STATE + state,
                state, stateExpireSeconds.longValue(), TimeUnit.SECONDS);
        WxWebLoginBiz loginBiz = yunGouWxApiClient.getWebLogin(
                mchId, buildWechatCallbackUrl(state), params, key);

        return loginBiz;
    }

    /** 为每次授权生成独立且不可预测的 state。 */
    private String generateState() {
        return generateRandomString(32);
    }

    private String buildWechatCallbackUrl(String state) {
        return UriComponentsBuilder.fromUriString(wechatCallbackUrl)
                .replaceQueryParam("state", state).build().encode().toUriString();
    }

    /**
     * 验证state参数
     */
    private boolean verifyState(String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }
        
        // 原子消费，两个并发回调不能重复使用同一个 state。
        Object cachedState = redisTemplate.opsForValue().getAndDelete(CacheConstants.Auth.WECHAT_STATE + state);
        return state.equals(cachedState);
    }

    @Override
    public String handleWechatCallback(String code, String state) throws UnsupportedEncodingException {
        if (!verifyState(state)) {
            log.warn("微信OAuth回调 - state校验失败");
            return buildErrorRedirectUrl("安全验证失败，请重新扫码登录");
        }

        // 验证授权码
        if (!StringUtils.hasText(code)) {
            log.warn("微信OAuth回调 - 用户未同意授权或授权码为空");
            return buildErrorRedirectUrl("😭您未同意授权");
        }
        
        // 获取微信用户信息
        WxOauthInfo wxOauthInfo;
        try {
            wxOauthInfo = yunGouWxApiClient.getWxOauthInfo(mchId, code, key);
            log.info("微信OAuth回调 - 成功获取微信用户信息");
        } catch (Exception e) {
            log.error("微信OAuth回调 - 获取微信用户信息失败: {}", e.getMessage(), e);
            return buildErrorRedirectUrl("微信授权失败，请重试😖");
        }
        
        String openId = wxOauthInfo.getOpenId();
        String unionId = wxOauthInfo.getWxUserInfo().getUnionid();
        String nickname = wxOauthInfo.getWxUserInfo().getNickname();
        String headimgurl = wxOauthInfo.getWxUserInfo().getHeadimgurl();
        
        // 查找或创建用户
        UserDO user = findOrCreateUser(openId, unionId, nickname, headimgurl);
        if (user == null) {
            log.error("微信OAuth回调 - 用户创建/查找失败");
            return buildErrorRedirectUrl("登录失败，请联系管理员");
        }
        
        // 检查用户状态
        if (!SystemConstants.Status.ACTIVE.equals(user.getStatus())) {
            log.warn("微信OAuth回调 - 用户已被禁用，userId: {}", user.getId());
            return buildErrorRedirectUrl("账号已被禁用");
        }
        
        // 执行登录，生成Sa-Token
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();
        String refreshToken = generateRefreshToken();
        storeRefreshToken(user.getId(), refreshToken);
        long tokenTimeout = StpUtil.getTokenTimeout();
        log.info("微信OAuth回调 - 登录成功，userId: {}", user.getId());
        
        // 缓存用户角色信息
        List<Long> roleIds = iSysUserRoleService.getRoleIdsByUserId(user.getId());
        String userRoleCacheKey = CacheConstants.Auth.USER_ROLE + user.getId();
        redisTemplate.opsForValue().set(userRoleCacheKey, roleIds);
        
        // 生成临时ticket（用于前端换取token）
        String ticket = IdUtil.simpleUUID();
        String ticketKey = CacheConstants.Auth.WECHAT_TICKET + ticket;
        
        // 将登录信息存入Redis
        JSONObject loginData = new JSONObject();
        loginData.put("userId", user.getId());
        loginData.put("accessToken", accessToken);
        loginData.put("refreshToken", refreshToken);
        loginData.put("tokenTimeout", tokenTimeout);
        
        redisTemplate.opsForValue().set(
            ticketKey, 
            loginData.toJSONString(), 
            ticketExpireSeconds.longValue(), 
            TimeUnit.SECONDS
        );
        String stateLoginKey = CacheConstants.Auth.WECHAT_STATE_LOGIN + state;
        redisTemplate.opsForValue().set(
            stateLoginKey,
            ticket,
            ticketExpireSeconds.longValue(),
            TimeUnit.SECONDS
        );
        
        log.info("微信OAuth回调 - 已生成ticket，有效期: {}秒", ticketExpireSeconds);
        
        // 构建重定向URL（返回前端）
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .fragment("ticket=" + ticket).build().encode().toUriString();
    }
    
    /**
     * 查找或创建用户
     */
    private UserDO findOrCreateUser(String openId, String unionId, String nickname, String headimgurl) {
        // 查询社交账号是否已存在
        SocialDO social = iSocialService.findByOpenIdAndPlatform(openId, TYPE);
        UserDO user;

        if (social == null) {
            // 新用户注册流程 — 使用编程式事务保证 social/user/role 三表原子写入
            log.info("微信OAuth回调 - 新用户注册，openId: {}", DesensitizeUtils.desensitizeCustom(openId, 4, 4));

            try {
                user = transactionTemplate.execute(status -> {
                    String random10 = generateRandomString(10);
                    log.info("随机生成的username为:{}", random10);

                    // 创建社交账号记录
                    SocialDO newSocial = new SocialDO()
                            .setOpenId(openId)
                            .setUsername(random10)
                            .setNickname(nickname)
                            .setAvatar(headimgurl)
                            .setUnionId(unionId)
                            .setPlatform(TYPE);
                    iSocialService.save(newSocial);

                    // 创建用户账号
                    UserDO newUser = new UserDO()
                            .setOpenid(openId)
                            .setUsername(random10)
                            .setNickname(nickname)
                            .setAvatar(headimgurl)
                            .setPassword(PasswordUtil.encrypt(generateRandomString(20)))
                            .setStatus(SystemConstants.Status.ACTIVE);
                    userService.save(newUser);
                    log.info("微信OAuth回调 - 创建用户成功，userId: {}", newUser.getId());

                    // 分配默认角色
                    assignDefaultRoles(newUser.getId());

                    return newUser;
                });
            } catch (Exception e) {
                log.error("微信OAuth回调 - 创建用户失败: {}", e.getMessage(), e);
                return null;
            }
        } else {
            // 老用户登录流程
            log.info("微信OAuth回调 - 老用户登录，openId: {}", DesensitizeUtils.desensitizeCustom(openId, 4, 4));
            
            // 通过用户名查找用户
            String socialUsername = social.getUsername();
            if (StringUtils.hasText(socialUsername)) {
                user = userService.getUserByUsername(socialUsername);
            } else {
                user = null;
            }
            
            // 如果通过用户名找不到，尝试通过openid查找
            if (user == null) {
                user = userService.lambdaQuery()
                        .eq(UserDO::getOpenid, openId)
                        .one();
            }
            
            // 如果还是找不到用户，数据不一致
            if (user == null) {
                log.error("微信OAuth回调 - 社交账号存在但找不到对应用户，openId: {}", DesensitizeUtils.desensitizeCustom(openId, 4, 4));
                return null;
            }
            
            // 更新用户信息
            try {
                user.setLastLoginTime(LocalDateTime.now())
                        .setNickname(nickname)
                        .setAvatar(headimgurl)
                ;
                userService.updateById(user);
            } catch (Exception e) {
                log.error("微信OAuth回调 - 更新用户信息失败: {}", e.getMessage(), e);
                // 更新失败不影响登录
            }
        }
        
        return user;
    }
    
    /**
     * 分配默认角色
     */
    private void assignDefaultRoles(Long userId) {
        if (wechatDefaultRoleId == null) {
            log.warn("微信OAuth回调 - 未配置默认角色ID");
            return;
        }
        UserRoleDO userRole = new UserRoleDO()
                .setUserId(userId)
                .setRoleId(wechatDefaultRoleId);
        iSysUserRoleService.save(userRole);
        log.info("微信OAuth回调 - 分配默认角色成功，角色ID: {}", wechatDefaultRoleId);
    }
    
    /**
     * 构建错误重定向URL
     */
    private String buildErrorRedirectUrl(String errorMsg) throws UnsupportedEncodingException {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .replaceQueryParam("error", errorMsg).build().encode().toUriString();
    }

    @Override
    public LoginVO exchangeTicket(String ticket) {
        log.info("微信扫码登录 - 使用ticket换取登录信息");
        
        // 参数校验
        if (!StringUtils.hasText(ticket)) {
            throw new IceeBootException("ticket不能为空");
        }
        
        // 从Redis获取登录数据
        String ticketKey = CacheConstants.Auth.WECHAT_TICKET + ticket;
        String loginDataStr = (String) redisTemplate.opsForValue().getAndDelete(ticketKey);
        
        if (!StringUtils.hasText(loginDataStr)) {
            log.error("微信扫码登录 - ticket已过期或不存在");
            throw new IceeBootException("登录凭证已过期，请重新扫码登录");
        }
        
        // ticket 已通过 getAndDelete 原子消费，避免并发重复兑换。
        log.info("微信扫码登录 - 成功获取登录数据并删除ticket");
        
        // 解析登录数据
        JSONObject loginData;
        try {
            loginData = JSONObject.parseObject(loginDataStr);
        } catch (Exception e) {
            log.error("微信扫码登录 - 解析登录数据失败: {}", e.getMessage(), e);
            throw new IceeBootException("登录数据异常");
        }
        
        Long userId = loginData.getLong("userId");
        String accessToken = loginData.getString("accessToken");
        String refreshToken = loginData.getString("refreshToken");
        Long tokenTimeout = loginData.getLong("tokenTimeout");
        
        if (userId == null || !StringUtils.hasText(accessToken)) {
            log.error("微信扫码登录 - 登录数据不完整");
            throw new IceeBootException("登录数据异常");
        }
        
        // 验证token是否有效
        try {
            Object tokenLoginId = StpUtil.getLoginIdByToken(accessToken);
            Long tokenUserId = tokenLoginId == null ? null : Long.valueOf(tokenLoginId.toString());
            if (tokenUserId == null || !tokenUserId.equals(userId)) {
                log.error("微信扫码登录 - token验证失败");
                throw new IceeBootException("登录状态异常，请重新登录");
            }
        } catch (Exception e) {
            log.error("微信扫码登录 - token验证失败: {}", e.getMessage(), e);
            throw new IceeBootException("登录状态异常，请重新登录");
        }
        
        // 获取用户信息
        SysUserVO userVO = userService.getUserById(userId);
        if (userVO == null) {
            log.error("微信扫码登录 - 用户不存在，userId: {}", userId);
            throw new IceeBootException(ResponseMessage.Auth.USER_NOT_EXIST);
        }
        
        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = generateRefreshToken();
            storeRefreshToken(userId, refreshToken);
        }
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUser(userVO);
        loginVO.setExpires(tokenTimeout);
        loginVO.setTokenType("Bearer");
        
        log.info("微信扫码登录 - 换取登录信息成功，userId: {}", userId);
        return loginVO;
    }

    @Override
    public Map<String, Object> checkLogin(String code) {
        Map<String, Object> data = new HashMap<>();
        String stateLoginKey = CacheConstants.Auth.WECHAT_STATE_LOGIN + code;
        Object ticketObj = redisTemplate.opsForValue().get(stateLoginKey);
        if (ticketObj != null) {
            data.put("loggedIn", true);
            data.put("ticket", ticketObj.toString());
            return data;
        }
        data.put("loggedIn", false);
        return data;
    }


    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    private String generateRefreshToken() {
        return IdUtil.simpleUUID();
    }

    private String consumeRefreshToken(String refreshToken) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setResultType(String.class);
        script.setScriptText(REFRESH_TOKEN_CONSUME_SCRIPT);

        String refreshKey = CacheConstants.Auth.REFRESH_TOKEN + refreshToken;
        String consumedKey = CacheConstants.Auth.REFRESH_TOKEN_CONSUMED + refreshToken;

        Object result = redisTemplate.execute(
                script,
                new StringRedisSerializer(),
                new StringRedisSerializer(),
                List.of(refreshKey, consumedKey),
                String.valueOf(REFRESH_TOKEN_CONSUMED_TTL_SECONDS)
        );

        return result != null ? result.toString() : null;
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        redisTemplate.execute(REPLACE_REFRESH_TOKEN_SCRIPT,
                List.of(getUserRefreshTokenKey(userId), CacheConstants.Auth.REFRESH_TOKEN + refreshToken),
                refreshToken, userId, refreshTokenExpireSeconds,
                CacheConstants.Auth.REFRESH_TOKEN, CacheConstants.Auth.REFRESH_TOKEN_CONSUMED);
    }

    private void revokeUserRefreshToken(Long userId) {
        if (userId == null) return;
        redisTemplate.execute(REVOKE_REFRESH_TOKEN_SCRIPT,
                List.of(getUserRefreshTokenKey(userId)),
                CacheConstants.Auth.REFRESH_TOKEN, CacheConstants.Auth.REFRESH_TOKEN_CONSUMED);
    }

    private String getUserRefreshTokenKey(Long userId) {
        return CacheConstants.Auth.USER_REFRESH_TOKEN + userId;
    }

    /**
     * 记录登录失败，达到阈值则锁定账号
     */
    private void recordLoginFailure(String username) {
        String failKey = CacheConstants.Auth.LOGIN_FAIL_COUNT + username;
        Long failCount = redisTemplate.opsForValue().increment(failKey);
        if (failCount != null && failCount == 1) {
            redisTemplate.expire(failKey, LOGIN_FAIL_WINDOW_SECONDS, TimeUnit.SECONDS);
        }
        if (failCount != null && failCount >= MAX_LOGIN_FAIL_COUNT) {
            String lockedKey = CacheConstants.Auth.LOGIN_LOCKED + username;
            redisTemplate.opsForValue().set(lockedKey, "1", LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
            redisTemplate.delete(failKey);
            log.warn("账号 {} 登录失败{}次，已锁定{}秒", username, failCount, LOGIN_LOCK_SECONDS);
        }
    }

    /**
     * 登录成功后清除失败计数
     */
    private void clearLoginFailure(String username) {
        redisTemplate.delete(CacheConstants.Auth.LOGIN_FAIL_COUNT + username);
        redisTemplate.delete(CacheConstants.Auth.LOGIN_LOCKED + username);
    }
}
