/**
 * 认证服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.system;

import com.iceeboot.framework.dto.create.system.LoginDTO;
import com.iceeboot.framework.vo.system.LoginVO;
import com.iceeboot.framework.vo.system.SysUserVO;

/**
 * 认证服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface IAuthService {

    /**
     * 用户登录
     *
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前用户信息
     *
     * @return 用户信息
     */
    SysUserVO getCurrentUser();

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return 新的登录信息
     */
    LoginVO refresh(String refreshToken);

    /**
     * 微信登录
     *
     * @param code 微信授权码
     * @return 登录结果
     */
    LoginVO wechatLogin(String code);
    
    /**
     * 检查登录状态
     *
     * @return 是否已登录
     */
    boolean checkLogin();
}