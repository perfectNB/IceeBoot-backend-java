/**
 * 角色视图对象
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
 * 角色视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysRoleVO {

    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色编码")
    private String code;

    @Schema(description = "角色描述")
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

    @Schema(description = "权限树")
    private List<SysPermissionVO> permissionTree;

    @Schema(description = "菜单树")
    private List<SysMenuVO> menuTree;

    @Schema(description = "用户数量")
    private Integer userCount;
}