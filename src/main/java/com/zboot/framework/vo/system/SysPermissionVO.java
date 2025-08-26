/**
 * 权限视图对象
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
 * 权限视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysPermissionVO {

    @Schema(description = "权限ID")
    private Long id;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "权限编码")
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

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    @Schema(description = "子权限列表")
    private List<SysPermissionVO> children;

    @Schema(description = "是否拥有该权限")
    private Boolean hasPermission;
}