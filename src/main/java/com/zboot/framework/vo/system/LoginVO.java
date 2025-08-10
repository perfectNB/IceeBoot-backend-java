/**
 * 登录响应视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.vo.system;

import lombok.Data;

/**
 * 登录响应视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LoginVO {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private SysUserVO user;

    /**
     * 过期时间（秒）
     */
    private Long expires;

    /**
     * 令牌类型
     */
    private String tokenType = "Bearer";
}