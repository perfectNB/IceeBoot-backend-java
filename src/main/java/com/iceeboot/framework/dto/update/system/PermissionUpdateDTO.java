package com.iceeboot.framework.dto.update.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 权限更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "权限更新数据传输对象")
public class PermissionUpdateDTO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "权限名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "权限名称不能为空")
    private String name;

    @Schema(description = "权限编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "权限编码不能为空")
    private String code;

    @Schema(description = "权限类型")
    private String type;

    @Schema(description = "父级ID")
    private Long parentId;

    @Schema(description = "路径")
    private String path;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "权限描述")
    private String description;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "排序")
    private Integer sort;
}