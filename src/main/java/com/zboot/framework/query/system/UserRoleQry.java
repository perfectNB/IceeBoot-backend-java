/**
 * 用户角色关联查询对象
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
 * 用户角色关联查询对象
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户角色关联查询对象")
public class UserRoleQry extends BaseQuery {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID")
    private Long roleId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;
}