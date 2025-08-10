/**
 * 角色数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.dto.system;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 角色数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
public class SysRoleDTO {

    /**
     * 角色ID
     */
    private Long id;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    private String name;

    /**
     * 角色编码
     */
    @NotBlank(message = "角色编码不能为空")
    private String code;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 权限ID列表
     */
    private List<Long> permissionIds;
}