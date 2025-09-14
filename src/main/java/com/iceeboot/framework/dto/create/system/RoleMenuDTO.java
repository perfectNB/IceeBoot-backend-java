/**
 * 角色菜单关联数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.dto.create.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 角色菜单关联数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@Schema(description = "角色菜单关联数据传输对象")
public class RoleMenuDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "角色ID")
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "菜单ID")
    @NotNull(message = "菜单ID不能为空")
    private Long menuId;
}