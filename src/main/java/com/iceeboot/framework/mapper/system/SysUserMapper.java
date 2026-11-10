/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    /**
     * 使用悲观锁查询用户（用于并发更新用户关联数据）
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM t_user WHERE id = #{id} FOR UPDATE")
    UserDO selectByIdForUpdate(@Param("id") Long id);

}