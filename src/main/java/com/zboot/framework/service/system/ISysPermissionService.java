/**
 * 权限服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.PermissionDO;
import com.zboot.framework.dto.system.PermissionDTO;
import com.zboot.framework.query.system.SysPermissionQuery;
import com.zboot.framework.vo.system.SysPermissionVO;

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
    PageResult<SysPermissionVO> getPermissionPage(SysPermissionQuery query);

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
    boolean updatePermission(PermissionDTO permissionDTO);

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
     * 根据父级ID获取子权限列表
     *
     * @param parentId 父级ID
     * @return 权限列表
     */
    List<SysPermissionVO> getChildrenByParentId(Long parentId);
}