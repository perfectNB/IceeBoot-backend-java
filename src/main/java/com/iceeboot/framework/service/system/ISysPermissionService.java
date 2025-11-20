/**
 * 权限服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.system.PermissionDTO;
import com.iceeboot.framework.dto.create.system.SysPermissionQueryDTO;
import com.iceeboot.framework.dto.update.system.PermissionUpdateDTO;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.vo.system.SysPermissionVO;

import java.util.List;

/**
 * 权限服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface ISysPermissionService extends IService<PermissionDO> {

    /**
     * 分页查询权限列表
     *
     * @param query 查询条件
     * @return 权限分页列表
     */
    PageResult<SysPermissionVO> getPermissionPage(SysPermissionQueryDTO query);

    /**
     * 获取权限树
     *
     * @return 权限树列表
     */
    List<SysPermissionVO> getPermissionTree();

    /**
     * 根据ID获取权限详情
     *
     * @param id 权限ID
     * @return 权限详情
     */
    SysPermissionVO getPermissionById(Long id);

    /**
     * 新增权限
     *
     * @param permissionDTO 权限信息
     * @return 是否成功
     */
    boolean addPermission(PermissionDTO permissionDTO);

    /**
     * 更新权限
     *
     * @param permissionDTO 权限信息
     * @return 是否成功
     */
    boolean updatePermission(PermissionUpdateDTO permissionDTO);

    /**
     * 删除权限
     *
     * @param ids 权限ID列表
     * @return 是否成功
     */
    boolean deletePermissions(List<Long> ids);

    /**
     * 修改权限状态
     *
     * @param id 权限ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);

    /**
     * 根据权限编码获取权限
     *
     * @param code 权限编码
     * @return 权限信息
     */
    PermissionDO getPermissionByCode(String code);

    /**
     * 检查权限编码是否存在
     *
     * @param code 权限编码
     * @param id 权限ID（排除自己）
     * @return 是否存在
     */
    boolean existsCode(String code, Long id);

    /**
     * 根据用户ID获取权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<PermissionDO> getPermissionsByUserId(Long userId);

    /**
     * 根据角色ID获取权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<PermissionDO> getPermissionsByRoleId(Long roleId);

    /**
     * 获取子权限
     *
     * @param parentId 父级权限ID
     * @return 子权限列表
     */
    List<SysPermissionVO> getChildrenByParentId(Long parentId);

    /**
     * 根据权限ID列表获取权限编码列表
     *
     * @param permissionIds 权限ID列表
     * @return 权限编码列表
     */
    List<String> getPermissionCodesByIds(List<Long> permissionIds);

    /**
     * 根据角色ID获取权限树（包含权限拥有状态）
     *
     * @param roleId 角色ID
     * @return 权限树列表
     */
    List<SysPermissionVO> getPermissionTreeByRoleId(Long roleId);
}