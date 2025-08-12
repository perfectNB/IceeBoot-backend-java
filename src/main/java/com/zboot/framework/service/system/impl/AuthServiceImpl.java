/**
 * 认证服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.service.system.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.framework.dto.system.LoginDTO;
import com.zboot.framework.entity.system.UserDO;
import com.zboot.framework.service.system.IAuthService;
import com.zboot.framework.service.system.ISysUserService;
import com.zboot.framework.vo.system.LoginVO;
import com.zboot.framework.vo.system.SysUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Service
public class AuthServiceImpl implements IAuthService {

    @Autowired
    private ISysUserService userService;



    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 参数校验
        if (!StringUtils.hasText(loginDTO.getUsername()) || !StringUtils.hasText(loginDTO.getPassword())) {
            throw new RuntimeException("用户名或密码不能为空");
        }

        // 查询用户
        UserDO user = userService.getUserByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new RuntimeException(ResponseMessage.USER_NOT_EXIST);
        }

        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            throw new RuntimeException("用户已被禁用");
        }

        // 验证密码
        if (!loginDTO.getPassword().equals(user.getPassword())) {
            throw new RuntimeException(ResponseMessage.PASSWORD_ERROR);
        }

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        userService.updateById(user);

        // 生成token
        StpUtil.login(user.getId());
        String accessToken = StpUtil.getTokenValue();
        String refreshToken = StpUtil.getTokenValue(); // 简化处理，实际应该生成不同的refresh token

        // 获取用户信息
        SysUserVO userVO = userService.getUserById(user.getId());

        // 构建返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUser(userVO);
        loginVO.setExpires(StpUtil.getTokenTimeout());
        loginVO.setTokenType("Bearer");

        return loginVO;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public SysUserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return userService.getUserById(userId);
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        // 验证refresh token
        if (!StringUtils.hasText(refreshToken)) {
            throw new RuntimeException("刷新令牌不能为空");
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
        throw new RuntimeException("微信登录功能暂未实现");
    }

}