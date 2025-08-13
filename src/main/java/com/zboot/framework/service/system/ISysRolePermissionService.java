/**
 * 角色权限关联服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.RolePermissionDO;
import com.zboot.framework.dto.system.RolePermissionDTO;
import com.zboot.framework.query.system.RolePermissionQry;
import com.zboot.framework.vo.system.RolePermissionVO;

import java.util.List;

/**
 * 角色权限关联服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
public interface ISysRolePermissionService extends IService<RolePermissionDO> {

    /**
     * 分页查询角色权限关联列表
     *
     * @param query 查询条件
     * @return 角色权限关联分页列表
     */
    PageResult<RolePermissionVO> getRolePermissionPage(RolePermissionQry query);

    /**
     * 根据ID获取角色权限关联详情
     *
     * @param id 角色权限关联ID
     * @return 角色权限关联详情
     */
    RolePermissionVO getRolePermissionById(Long id);

    /**
     * 新增角色权限关联
     *
     * @param rolePermissionDTO 角色权限关联信息
     * @return 是否成功
     */
    boolean addRolePermission(RolePermissionDTO rolePermissionDTO);

    /**
     * 修改角色权限关联
     *
     * @param rolePermissionDTO 角色权限关联信息
     * @return 是否成功
     */
    boolean updateRolePermission(RolePermissionDTO rolePermissionDTO);

    /**
     * 删除角色权限关联
     *
     * @param ids 角色权限关联ID列表
     * @return 是否成功
     */
    boolean deleteRolePermissions(List<Long> ids);

    /**
     * 根据角色ID获取权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> getPermissionIdsByRoleId(Long roleId);

    /**
     * 根据权限ID获取角色ID列表
     *
     * @param permissionId 权限ID
     * @return 角色ID列表
     */
    List<Long> getRoleIdsByPermissionId(Long permissionId);

    /**
     * 为角色分配权限
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 是否成功
     */
    boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    /**
     * 移除角色的所有权限
     *
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean removeAllPermissionsByRoleId(Long roleId);
}