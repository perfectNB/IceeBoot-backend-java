/**
 * 用户数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.dto.system;

import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 用户数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysUserDTO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 状态
     */
    private String status;

    /**
     * 角色ID列表
     */
    private List<Long> roleIds;

    /**
     * 角色列表
     */
    private List<String> roles;
}