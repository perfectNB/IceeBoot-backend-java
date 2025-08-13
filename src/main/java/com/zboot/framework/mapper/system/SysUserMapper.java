/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.UserDO;
import com.zboot.framework.entity.system.PermissionDO;
import com.zboot.framework.entity.system.RoleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface SysUserMapper extends BaseMapper<UserDO> {

    /**
     * 根据用户ID查询权限列表
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    List<PermissionDO> selectPermissionsByUserId(Long userId);

    /**
     * 根据用户ID查询角色列表
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    List<RoleDO> selectRolesByUserId(Long userId);

}