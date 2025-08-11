/**
 * 角色权限关联Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysRolePermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关联Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermissionDO> {

    /**
     * 根据角色ID查询权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID查询角色ID列表
     *
     * @param permissionId 权限ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByPermissionId(@Param("permissionId") Long permissionId);

    /**
     * 根据用户ID查询权限ID列表
     *
     * @param userId 用户ID
     * @return 权限ID列表
     */
    List<Long> selectPermissionIdsByUserId(@Param("userId") Long userId);

    /**
     * 批量插入角色权限关联
     *
     * @param rolePermissions 角色权限关联列表
     * @return 影响行数
     */
    int insertBatch(@Param("rolePermissions") List<SysRolePermissionDO> rolePermissions);

    /**
     * 根据角色ID删除角色权限关联
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据权限ID删除角色权限关联
     *
     * @param permissionId 权限ID
     * @return 影响行数
     */
    int deleteByPermissionId(@Param("permissionId") Long permissionId);
}