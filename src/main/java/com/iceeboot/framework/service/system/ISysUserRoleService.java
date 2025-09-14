/**
 * 用户角色关联服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.entity.system.UserRoleDO;
import com.iceeboot.framework.dto.create.system.UserRoleDTO;
import com.iceeboot.framework.query.system.UserRoleQuery;
import com.iceeboot.framework.vo.system.UserRoleVO;

import java.util.List;

/**
 * 用户角色关联服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
public interface ISysUserRoleService extends IService<UserRoleDO> {

    /**
     * 分页查询用户角色关联列表
     *
     * @param query 查询条件
     * @return 用户角色关联分页列表
     */
    PageResult<UserRoleVO> getUserRolePage(UserRoleQuery query);

    /**
     * 根据ID获取用户角色关联详情
     *
     * @param id 用户角色关联ID
     * @return 用户角色关联详情
     */
    UserRoleVO getUserRoleById(Long id);

    /**
     * 新增用户角色关联
     *
     * @param userRoleDTO 用户角色关联信息
     * @return 是否成功
     */
    boolean addUserRole(UserRoleDTO userRoleDTO);

    /**
     * 修改用户角色关联
     *
     * @param userRoleDTO 用户角色关联信息
     * @return 是否成功
     */
    boolean updateUserRole(UserRoleDTO userRoleDTO);

    /**
     * 删除用户角色关联
     *
     * @param ids 用户角色关联ID列表
     * @return 是否成功
     */
    boolean deleteUserRoles(List<Long> ids);

    /**
     * 根据用户ID获取角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 根据角色ID获取用户ID列表
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> getUserIdsByRoleId(Long roleId);

    /**
     * 为用户分配角色
     *
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * @return 是否成功
     */
    boolean assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 移除用户的所有角色
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean removeAllRolesByUserId(Long userId);
}