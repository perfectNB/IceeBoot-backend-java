package com.zboot.framework.dto.update.system;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 角色更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "角色更新数据传输对象")
public class RoleUpdateDTO {

    @Schema(description = "主键ID")
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    private String name;

    @NotBlank(message = "角色编码不能为空")
    private String code;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "权限ID列表")
    private List<Long> permissionIds;
}