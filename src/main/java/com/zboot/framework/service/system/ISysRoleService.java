/**
 * 角色服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.RoleDO;
import com.zboot.framework.dto.system.RoleDTO;
import com.zboot.framework.query.system.SysRoleQuery;
import com.zboot.framework.vo.system.SysRoleVO;

import java.util.List;

/**
 * 角色服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface ISysRoleService extends IService<RoleDO> {

    /**
     * 分页查询角色列表
     *
     * @param query 查询条件
     * @return 角色分页列表
     */
    PageResult<SysRoleVO> getRolePage(SysRoleQuery query);

    /**
     * 获取所有角色列表
     *
     * @return 角色列表
     */
    List<SysRoleVO> getAllRoles();

    /**
     * 根据ID获取角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    SysRoleVO getRoleById(Long id);

    /**
     * 新增角色
     *
     * @param roleDTO 角色信息
     * @return 是否成功
     */
    boolean addRole(RoleDTO roleDTO);

    /**
     * 更新角色
     *
     * @param roleDTO 角色信息
     * @return 是否成功
     */
    boolean updateRole(RoleDTO roleDTO);

    /**
     * 删除角色
     *
     * @param ids 角色ID列表
     * @return 是否成功
     */
    boolean deleteRoles(List<Long> ids);

    /**
     * 修改角色状态
     *
     * @param id 角色ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);

    /**
     * 根据角色编码获取角色
     *
     * @param code 角色编码
     * @return 角色信息
     */
    RoleDO getRoleByCode(String code);

    /**
     * 检查角色编码是否存在
     *
     * @param code 角色编码
     * @param excludeId 排除的角色ID
     * @return 是否存在
     */
    boolean existsCode(String code, Long excludeId);

    /**
     * 检查角色名称是否存在
     *
     * @param name 角色名称
     * @param excludeId 排除的角色ID
     * @return 是否存在
     */
    boolean existsName(String name, Long excludeId);

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<RoleDO> getRolesByUserId(Long userId);

    /**
     * 为用户分配角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 保存角色权限
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     * @return 是否成功
     */
    boolean saveRolePermissions(Long roleId, List<Long> permissionIds);

    /**
     * 保存角色菜单
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功
     */
    boolean saveRoleMenus(Long roleId, List<Long> menuIds);
}