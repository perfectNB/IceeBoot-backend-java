/**
 * 菜单数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 菜单数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysMenuDTO {

    @Schema(description = "菜单ID")
    private Long id;

    @Schema(description = "菜单标题", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单标题不能为空")
    private String title;

    @Schema(description = "菜单名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "菜单名称不能为空")
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
}