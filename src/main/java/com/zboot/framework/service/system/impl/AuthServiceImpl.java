/**
 * 认证服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.exception.ZbootException;
import com.zboot.common.utils.Md5Utils;
import com.zboot.common.utils.PasswordUtil;
import com.zboot.common.utils.TokenUtils;
import com.zboot.framework.dto.system.LoginDTO;
import com.zboot.framework.entity.system.UserDO;
import com.zboot.framework.service.system.*;
import com.zboot.framework.vo.system.LoginVO;
import com.zboot.framework.vo.system.SysUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 认证服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleMenuService iSysRoleMenuService;

    @Autowired
    private ISysRolePermissionService iSysRolePermissionService;

    @Autowired
    private ISysPermissionService iSysPermissionService;

    @Autowired
    private ISysMenuService iSysMenuService;

    @Autowired
    private IAuthCacheService authCacheService;


    @Override
    public LoginVO login(LoginDTO loginDTO) {

        // 参数校验
        if (!StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new ZbootException(ResponseMessage.USERNAME_OR_PASSWORD_EMPTY);
        }

        // 查询用户
        UserDO user = userService.getUserByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new ZbootException(ResponseMessage.USER_NOT_EXIST);
        }

        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new ZbootException("用户已被禁用");
        }


        // 验证密码
        if (!PasswordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new ZbootException(ResponseMessage.PASSWORD_ERROR);
        }

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);

        // 生成token
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();
        
        // 获取token信息
        long tokenTimeout = StpUtil.getTokenTimeout();
        
        // 获取用户信息
        SysUserVO userVO = userService.getUserById(user.getId());
//
//        // 获取用户菜单和权限
//        List<Map<String, Object>> userMenus = authCacheService.getUserMenus(user.getId());

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(accessToken); // 这里可以生成不同的refresh token
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
        // 验证refresh token
        if (!StringUtils.hasText(refreshToken)) {
            throw new ZbootException("刷新令牌不能为空");
        }

        Long userId = StpUtil.getLoginIdAsLong();
        UserDO user = userService.getById(userId);

        // 生成新token
        String accessToken = StpUtil.getTokenValue();
        String newRefreshToken = StpUtil.getTokenValue();

        // 获取用户信息
        SysUserVO userVO = userService.getUserById(user.getId());

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(newRefreshToken);
        loginVO.setUser(userVO);
        loginVO.setExpires(StpUtil.getTokenTimeout());
        loginVO.setTokenType("Bearer");

        return loginVO;
    }

    @Override
    public LoginVO wechatLogin(String code) {
        // 微信登录逻辑，暂时抛出异常
        throw new ZbootException("微信登录功能暂未实现");
    }



}