/**
 * 角色权限关联数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.dto.create.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 角色权限关联数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@Schema(description = "角色权限关联数据传输对象")
public class RolePermissionDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "权限ID")
    @NotNull(message = "权限ID不能为空")
    private Long permissionId;
}