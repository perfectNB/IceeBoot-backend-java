/**
 * 系统菜单实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.entity.system;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

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

    @Schema(description = "菜单标题")
    @TableField("title")
    private String title;

    @Schema(description = "菜单名称")
    @TableField("name")
    private String name;

    @Schema(description = "菜单路径")
    @TableField("path")
    private String path;

    @Schema(description = "组件路径")
    @TableField("component")
    private String component;

    @Schema(description = "图标")
    @TableField("icon")
    private String icon;

    @Schema(description = "父级ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "菜单类型（menu-菜单，button-按钮，api-接口，link-外链）")
    @TableField("type")
    private String type;

    @Schema(description = "是否隐藏")
    @TableField("hidden")
    private Boolean hidden;

    @Schema(description = "是否缓存")
    @TableField("keep_alive")
    private Boolean keepAlive;

    @Schema(description = "是否需要认证")
    @TableField("requires_auth")
    private Boolean requiresAuth;

    @Schema(description = "外链地址")
    @TableField("link")
    private String link;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "权限列表（逗号分隔）")
    @TableField("permissions")
    private String permissions;
}