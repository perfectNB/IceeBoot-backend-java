/**
 * 系统角色实体类
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

import java.util.List;

/**
 * 系统角色实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_role")
public class RoleDO extends ZbootDO {

    @Schema(description = "角色名称")
    @TableField("name")
    private String name;

    @Schema(description = "角色编码")
    @TableField("code")
    private String code;

    @Schema(description = "角色描述")
    @TableField("description")
    private String description;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "排序")
    @TableField("sort")
    private Integer sort;

    @Schema(description = "权限列表")
    @TableField(exist = false)
    private List<String> permissions;
}