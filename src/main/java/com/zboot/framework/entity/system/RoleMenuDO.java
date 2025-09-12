/**
 * 角色菜单关联实体类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色菜单关联实体类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_role_menu")
@Schema(description = "角色有的菜单")
public class RoleMenuDO extends ZbootDO {

    @Schema(description = "角色ID")
    @TableField("role_id")
    private Long roleId;

    @Schema(description = "菜单ID")
    @TableField("menu_id")
    private Long menuId;
}