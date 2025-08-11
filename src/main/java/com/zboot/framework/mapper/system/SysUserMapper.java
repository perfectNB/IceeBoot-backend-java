/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Mapper
public interface SysUserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户ID查询用户权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<String> selectRolesByUserId(@Param("userId") Long userId);

}