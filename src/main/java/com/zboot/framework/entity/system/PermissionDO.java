/**
 * 系统权限实体类
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
 * 系统权限实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_permission")
public class PermissionDO extends ZbootDO {

    @Schema(description = "权限名称")
    @TableField("name")
    private String name;

    @Schema(description = "权限编码")
    @TableField("code")
    private String code;

    @Schema(description = "权限类型（menu-菜单，button-按钮，api-接口）")
    @TableField("type")
    private String type;

    @Schema(description = "父级ID")
    @TableField("parent_id")
    private Long parentId;

    @Schema(description = "路径")
    @TableField("path")
    private String path;

    @Schema(description = "请求方法（GET、POST、PUT、DELETE、PATCH）")
    @TableField("method")
    private String method;

    @Schema(description = "权限描述")
    @TableField("description")
    private String description;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;
}