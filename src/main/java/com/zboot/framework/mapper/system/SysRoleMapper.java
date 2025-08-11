/**
 * 系统角色Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统角色Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRoleDO> {

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRoleDO> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 检查角色名称是否存在
     *
     * @param roleName 角色名称
     * @param roleId 角色ID（排除自己）
     * @return 是否存在
     */
    boolean checkRoleNameUnique(@Param("roleName") String roleName, @Param("roleId") Long roleId);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     * @param roleId 角色ID（排除自己）
     * @return 是否存在
     */
    boolean checkRoleCodeUnique(@Param("roleCode") String roleCode, @Param("roleId") Long roleId);

    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    List<String> selectPermissionsByRoleId(@Param("roleId") Long roleId);
}