/**
 * 角色权限关联实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 角色权限关联实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@TableName("sys_role_permission")
public class SysRolePermissionDO {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;

    /**
     * 权限ID
     */
    @TableField("permission_id")
    private Long permissionId;
}