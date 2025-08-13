/**
 * 角色菜单关联查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.query.system;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色菜单关联查询对象")
public class RoleMenuQry extends BaseQuery {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "菜单ID")
    private Long menuId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单路径")
    private String menuPath;
}