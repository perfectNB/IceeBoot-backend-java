/**
 * 用户角色关联Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysUserRoleDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关联Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRoleDO> {

    /**
     * 根据用户ID查询角色ID列表
     *
     * @param userId 用户ID
     * @return 角色ID列表
     */
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询用户ID列表
     *
     * @param roleId 角色ID
     * @return 用户ID列表
     */
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 批量插入用户角色关联
     *
     * @param userRoles 用户角色关联列表
     * @return 影响行数
     */
    int insertBatch(@Param("userRoles") List<SysUserRoleDO> userRoles);

    /**
     * 根据用户ID删除用户角色关联
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID删除用户角色关联
     *
     * @param roleId 角色ID
     * @return 影响行数
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
}