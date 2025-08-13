/**
 * 角色权限关联查询对象
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
 * 角色权限关联查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "角色权限关联查询对象")
public class RolePermissionQry extends BaseQuery {

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "权限ID")
    private Long permissionId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "权限名称")
    private String permissionName;

    @Schema(description = "权限编码")
    private String permissionCode;
}