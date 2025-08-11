/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysUserDO;
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
public interface SysUserMapper extends BaseMapper<SysUserDO> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    SysUserDO selectByUsername(@Param("username") String username);

    /**
     * 根据用户ID查询用户角色
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<String> selectRolesByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询用户权限
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @param userId 用户ID（排除自己）
     * @return 是否存在
     */
    boolean checkUsernameUnique(@Param("username") String username, @Param("userId") Long userId);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @param userId 用户ID（排除自己）
     * @return 是否存在
     */
    boolean checkEmailUnique(@Param("email") String email, @Param("userId") Long userId);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @param userId 用户ID（排除自己）
     * @return 是否存在
     */
    boolean checkPhoneUnique(@Param("phone") String phone, @Param("userId") Long userId);
}