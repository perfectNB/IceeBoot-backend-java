/**
 * 用户数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.create.system;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class UserDTO {

    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

//    @Schema(description = "角色列表")
//    private List<String> roles;
}