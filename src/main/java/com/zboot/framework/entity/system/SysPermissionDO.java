/**
 * 系统权限实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统权限实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermissionDO extends ZbootDO {

    /**
     * 权限名称
     */
    @TableField("name")
    private String name;

    /**
     * 权限编码
     */
    @TableField("code")
    private String code;

    /**
     * 权限类型（menu-菜单，button-按钮，api-接口）
     */
    @TableField("type")
    private String type;

    /**
     * 父级ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 路径
     */
    @TableField("path")
    private String path;

    /**
     * 请求方法（GET、POST、PUT、DELETE、PATCH）
     */
    @TableField("method")
    private String method;

    /**
     * 权限描述
     */
    @TableField("description")
    private String description;

    /**
     * 状态（active-激活，inactive-禁用）
     */
    @TableField("status")
    private String status;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 子权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysPermissionDO> children;
}