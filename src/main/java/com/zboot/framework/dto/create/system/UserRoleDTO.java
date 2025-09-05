/**
 * 用户角色关联数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.dto.create.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 用户角色关联数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@Schema(description = "用户角色关联数据传输对象")
public class UserRoleDTO {


    @Schema(description = "用户ID")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;
}