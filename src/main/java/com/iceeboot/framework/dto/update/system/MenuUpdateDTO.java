package com.iceeboot.framework.dto.update.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 菜单更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "菜单更新数据传输对象")
public class MenuUpdateDTO {

    @Schema(description = "主键ID")
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
    private List<String> permissions;

    @Schema(description = "外链地址")
    private String link;

    @Schema(description = "状态")
    private String status;
}