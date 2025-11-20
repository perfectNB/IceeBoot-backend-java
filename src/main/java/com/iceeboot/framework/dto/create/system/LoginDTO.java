/**
 * 登录数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.dto.create.system;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class LoginDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 验证码
     */
    private String captcha;

    /**
     * 验证码UUID
     */
    private String uuid;

    /**
     * 记住我
     */
    private Boolean rememberMe;
}