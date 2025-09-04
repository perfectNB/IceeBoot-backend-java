/**
 * 菜单视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.vo.system;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜单视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysMenuVO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "菜单标题")
    private String title;

    @Schema(description = "菜单名称")
    private String name;

    @Schema(description = "菜单路径")
    private String path;

    @Schema(description = "组件路径")
    private String component;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "菜单类型")
    private String type;

    @Schema(description = "是否隐藏")
    private Boolean hidden;

    @Schema(description = "是否缓存")
    private Boolean keepAlive;

    @Schema(description = "是否需要认证")
    private Boolean requiresAuth;

    @Schema(description = "权限列表")
    private String permissions;

    @Schema(description = "外链地址")
    private String link;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "子菜单列表")
    private List<SysMenuVO> children;

    @Schema(description = "是否拥有权限")
    private Boolean hasPermission;
}