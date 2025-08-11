/**
 * 系统权限Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysPermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统权限Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionDO> {

    /**
     * 根据用户ID查询权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<SysPermissionDO> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<SysPermissionDO> selectPermissionsByRoleId(@Param("roleId") Long roleId);

    /**
     * 检查权限编码是否存在
     *
     * @param permissionCode 权限编码
     * @param permissionId 权限ID（排除自己）
     * @return 是否存在
     */
    boolean checkPermissionCodeUnique(@Param("permissionCode") String permissionCode, @Param("permissionId") Long permissionId);

    /**
     * 根据父级ID查询子权限列表
     *
     * @param parentId 父级ID
     * @return 权限列表
     */
    List<SysPermissionDO> selectByParentId(@Param("parentId") Long parentId);
}