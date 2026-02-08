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

import java.io.UnsupportedEncodingException;
import java.util.Map;

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
     * 获取微信授权URL（带state参数）
     *
     * @return 微信授权URL
     */
    String getWechatAuthUrl();


    /**
     * 获取微信登录二维码
     *
     * @return 获取微信登录二维码
     */
    Object getWechatQr();

    /**
     * 微信OAuth回调处理（由微信服务器调用）
     *
     * @param code 微信授权码
     * @param state state参数（用于防CSRF攻击）
     * @return 重定向到前端的URL（携带ticket）
     */
    String handleWechatCallback(String code, String state) throws UnsupportedEncodingException;
    
    /**
     * 使用ticket换取登录信息
     *
     * @param ticket 临时登录凭证
     * @return 登录结果（包含token和用户信息）
     */
    LoginVO exchangeTicket(String ticket);
    
    /**
     * 检查登录状态
     *
     * @return 是否已登录
     */
    Map<String, Object> checkLogin(String code);
}