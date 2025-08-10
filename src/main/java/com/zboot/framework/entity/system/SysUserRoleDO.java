/**
 * 用户角色关联实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 用户角色关联实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@TableName("sys_user_role")
public class SysUserRoleDO {

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;
}