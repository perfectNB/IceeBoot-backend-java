/**
 * 系统菜单实体类
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
 * 系统菜单实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenuDO extends ZbootDO {

    /**
     * 菜单标题
     */
    @TableField("title")
    private String title;

    /**
     * 菜单名称
     */
    @TableField("name")
    private String name;

    /**
     * 菜单路径
     */
    @TableField("path")
    private String path;

    /**
     * 组件路径
     */
    @TableField("component")
    private String component;

    /**
     * 图标
     */
    @TableField("icon")
    private String icon;

    /**
     * 父级ID
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 菜单类型（menu-菜单，button-按钮，api-接口，link-外链）
     */
    @TableField("type")
    private String type;

    /**
     * 是否隐藏
     */
    @TableField("hidden")
    private Boolean hidden;

    /**
     * 是否缓存
     */
    @TableField("keep_alive")
    private Boolean keepAlive;

    /**
     * 是否需要认证
     */
    @TableField("requires_auth")
    private Boolean requiresAuth;

    /**
     * 外链地址
     */
    @TableField("link")
    private String link;

    /**
     * 状态（active-激活，inactive-禁用）
     */
    @TableField("status")
    private String status;

    /**
     * 权限列表（逗号分隔）
     */
    @TableField("permissions")
    private String permissions;

    /**
     * 子菜单列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysMenuDO> children;

    /**
     * 权限列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<String> permissionList;
}