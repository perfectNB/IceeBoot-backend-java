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
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.Md5Utils;
import com.iceeboot.common.utils.PasswordUtil;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.system.LoginDTO;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.SocialDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.entity.system.UserRoleDO;
import com.iceeboot.framework.service.system.*;
import com.iceeboot.framework.vo.system.LoginVO;
import com.iceeboot.framework.vo.system.SysUserVO;
import com.yungouos.pay.entity.WxOauthInfo;
import com.yungouos.pay.wxapi.WxApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
@Slf4j
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

    @Value("${YunGouOS.stateSecret}")
    private String stateSecret;

    @Value("${YunGouOS.ticketExpireSeconds}")
    private Integer ticketExpireSeconds;

    @Value("${YunGouOS.stateExpireSeconds:300}")
    private Integer stateExpireSeconds;

    @Value("${iceeboot.auth.refresh-token-expire-seconds:604800}")
    private Long refreshTokenExpireSeconds;

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysUserRoleService iSysUserRoleService;

    @Autowired
    private ISocialService iSocialService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ISysRoleService iSysRoleService;

    private static final Integer TYPE = SocialConstant.SOCIAL_TYPE_WECHAT;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final SecureRandom random = new SecureRandom();


    @Override
    public LoginVO login(LoginDTO loginDTO) {

        // 参数校验
        if (!StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new IceeBootException(ResponseMessage.Auth.USERNAME_OR_PASSWORD_EMPTY);
        }

        // 查询用户
        UserDO user = userService.getUserByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new IceeBootException(ResponseMessage.Auth.USER_NOT_EXIST);
        }

        // 检查用户状态
        if (!SystemConstants.Status.ACTIVE.equals(user.getStatus())) {
            throw new IceeBootException("用户已被禁用");
        }


        // 验证密码
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new IceeBootException(ResponseMessage.Auth.PASSWORD_ERROR);
        }

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
        TokenUtils.logout();
    }

    @Override
    public boolean checkLogin() {
        return TokenUtils.isLogin();
    }

    @Override
    public SysUserVO getCurrentUser() {
        Long userId = TokenUtils.getCurrentUserId();
        return userService.getUserById(userId);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new IceeBootException("刷新令牌不能为空");
        }

        String refreshKey = CacheConstants.Auth.REFRESH_TOKEN + refreshToken;
        Object cachedUserId = redisTemplate.opsForValue().get(refreshKey);
        if (cachedUserId == null) {
            throw new IceeBootException("刷新令牌已失效");
        }

        Long userId = Long.valueOf(cachedUserId.toString());
        UserDO user = userService.getById(userId);
        if (user == null) {
            throw new IceeBootException(ResponseMessage.Auth.USER_NOT_EXIST);
        }
        if (!SystemConstants.Status.ACTIVE.equals(user.getStatus())) {
            throw new IceeBootException("用户已被禁用");
        }

        StpUtil.login(userId);
        String accessToken = StpUtil.getTokenValue();
        String newRefreshToken = generateRefreshToken();
        storeRefreshToken(userId, newRefreshToken);
        redisTemplate.delete(refreshKey);

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
        String authUrl = WxApi.getWxOauthUrl(mchId, wechatCallbackUrl, type, params, key);
        
        log.info("微信扫码登录 - 授权URL生成成功");
        return authUrl;
    }
    
    /**
     * 生成state参数（基于时间戳和密钥的签名）
     */
    private String generateState() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String raw = timestamp + ":" + stateSecret;
        return Md5Utils.encrypt(raw);
    }
    
    /**
     * 验证state参数
     */
    private boolean verifyState(String state) {
        if (!StringUtils.hasText(state)) {
            return false;
        }
        
        // 从Redis验证state是否存在
        String stateKey = CacheConstants.Auth.WECHAT_STATE + state;
        String cachedState = (String) redisTemplate.opsForValue().get(stateKey);
        
        if (cachedState != null && cachedState.equals(state)) {
            // 验证成功后删除state（一次性使用）
            redisTemplate.delete(stateKey);
            return true;
        }
        
        return false;
    }

    @Override
    public String handleWechatCallback(String code, String state) throws UnsupportedEncodingException {
        log.info("微信OAuth回调 - 开始处理, code: {}, state: {}", code != null ? "***" : "null", state);
        
        if (state == null) {
            log.warn("微信OAuth回调 - state参数为空");
            return buildErrorRedirectUrl("安全验证失败，请重新扫码登录✌️");
        }
        
        if (!verifyState(state)) {
            log.error("微信OAuth回调 - state验证失败，state值: {}", state);
            return buildErrorRedirectUrl("安全验证失败，请重新扫码登录✌️");
        }
        log.info("微信OAuth回调 - state验证成功");
        
        // 验证授权码
        if (!StringUtils.hasText(code)) {
            log.warn("微信OAuth回调 - 用户未同意授权或授权码为空");
            return buildErrorRedirectUrl("😭您未同意授权");
        }
        
        // 获取微信用户信息
        WxOauthInfo wxOauthInfo;
        try {
            wxOauthInfo = WxApi.getWxOauthInfo(mchId, code, key);
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
        
        log.info("微信OAuth回调 - 已生成ticket，有效期: {}秒", ticketExpireSeconds);
        
        // 构建重定向URL（返回前端）
        String redirectUrl = frontendUrl + "/wechat/callback?ticket=" + ticket;
        log.info("微信OAuth回调 - 重定向到前端: {}", redirectUrl);
        
        return redirectUrl;
    }
    
    /**
     * 查找或创建用户
     */
    private UserDO findOrCreateUser(String openId, String unionId, String nickname, String headimgurl) {
        // 查询社交账号是否已存在
        SocialDO social = iSocialService.findByOpenIdAndPlatform(openId, TYPE);
        UserDO user;
        
        if (social == null) {
            // 新用户注册流程
            log.info("微信OAuth回调 - 新用户注册，openId: {}", openId);
            
            try {
                String random10 = generateRandomString(10);
                log.info("随机生成的username为:{}",random10);
                // 创建社交账号记录
                social = new SocialDO()
                        .setOpenId(openId)
                        .setUsername(random10)
                        .setNickname(nickname)
                        .setAvatar(headimgurl)
                        .setUnionId(unionId)
                        .setPlatform(TYPE);
                iSocialService.save(social);
                
                // 创建用户账号
                user = new UserDO()
                        .setOpenid(openId)
                        .setUsername(random10)
                        .setNickname(nickname)
                        .setAvatar(headimgurl)
                        .setPassword(PasswordUtil.encrypt(generateRandomString(20)))
                        .setStatus(SystemConstants.Status.ACTIVE);
                
                userService.save(user);
                log.info("微信OAuth回调 - 创建用户成功，userId: {}", user.getId());
                
                // 分配默认角色
                assignDefaultRoles(user.getId());
                

                
            } catch (Exception e) {
                log.error("微信OAuth回调 - 创建用户失败: {}", e.getMessage(), e);
                return null;
            }
        } else {
            // 老用户登录流程
            log.info("微信OAuth回调 - 老用户登录，openId: {}", openId);
            
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
                log.error("微信OAuth回调 - 社交账号存在但找不到对应用户，openId: {}", openId);
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
        try {
            List<RoleDO> defaultRoles = iSysRoleService.getDefaultRoles();
            if (defaultRoles != null && !defaultRoles.isEmpty()) {
                for (RoleDO role : defaultRoles) {
                    UserRoleDO userRole = new UserRoleDO()
                            .setUserId(userId)
                            .setRoleId(role.getId());
                    iSysUserRoleService.save(userRole);
                }
                log.info("微信OAuth回调 - 分配默认角色成功，角色数量: {}", defaultRoles.size());
            } else {
                log.warn("微信OAuth回调 - 没有配置默认角色");
            }
        } catch (Exception e) {
            log.error("微信OAuth回调 - 分配默认角色失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 构建错误重定向URL
     */
    private String buildErrorRedirectUrl(String errorMsg) throws UnsupportedEncodingException {
        return frontendUrl + "/wechat/callback?error=" + URLEncoder.encode(errorMsg, "UTF-8");
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
        String loginDataStr = (String) redisTemplate.opsForValue().get(ticketKey);
        
        if (!StringUtils.hasText(loginDataStr)) {
            log.error("微信扫码登录 - ticket已过期或不存在");
            throw new IceeBootException("登录凭证已过期，请重新扫码登录");
        }
        
        // 删除ticket（一次性使用）
        redisTemplate.delete(ticketKey);
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
            Long tokenUserId = (Long) StpUtil.getLoginIdByToken(accessToken);
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
            throw new IceeBootException("用户不存在");
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

    private void storeRefreshToken(Long userId, String refreshToken) {
        String refreshKey = CacheConstants.Auth.REFRESH_TOKEN + refreshToken;
        redisTemplate.opsForValue().set(refreshKey, userId, refreshTokenExpireSeconds, TimeUnit.SECONDS);
    }

    public static void main(String[] args) {
        String random20 = generateRandomString(20);
        System.out.println(random20); // 例如：aB3xK9LmQw2RtY7nVpE8
    }


}
