/**
 * 用户服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.dto.create.system.UserDTO;
import com.iceeboot.framework.dto.update.system.UserUpdateDTO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.query.system.SysUserQuery;
import com.iceeboot.framework.vo.system.SysUserVO;
import com.iceeboot.framework.vo.system.SysUserListVO;

import java.util.List;

/**
 * 用户服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface ISysUserService extends IService<UserDO> {

    /**
     * 分页查询用户列表
     *
     * @param query 查询条件
     * @return 用户分页列表
     */
    PageResult<SysUserListVO> getUserPage(SysUserQuery query);

    /**
     * 根据ID获取用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    SysUserVO getUserById(Long id);

    /**
     * 新增用户
     *
     * @param userDTO 用户信息
     * @return 是否成功
     */
    boolean addUser(UserDTO userDTO);

    /**
     * 更新用户
     *
     * @param userDTO 用户信息
     * @return 是否成功
     */
    boolean updateUser(UserUpdateDTO userDTO);

    /**
     * 删除用户
     *
     * @param ids 用户ID列表
     * @return 是否成功
     */
    boolean deleteUsers(List<Long> ids);

    /**
     * 重置密码
     *
     * @param id 用户ID
     * @param newPassword 新密码
     * @return 是否成功
     */
    boolean resetPassword(Long id, String newPassword);

    /**
     * 修改用户状态
     *
     * @param id 用户ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    UserDO getUserByUsername(String username);

    /**
     * 根据手机号获取用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    UserDO getUserByPhone(String phone);

    /**
     * 根据邮箱获取用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    UserDO getUserByEmail(String email);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean existsUsername(String username, Long excludeId);

    /**
     * 检查手机号是否存在
     *
     * @param phone 手机号
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean existsPhone(String phone, Long excludeId);

    /**
     * 检查邮箱是否存在
     *
     * @param email 邮箱
     * @param excludeId 排除的用户ID
     * @return 是否存在
     */
    boolean existsEmail(String email, Long excludeId);

    /**
     * 获取用户权限列表
     *
     * @param userId 用户ID
     * @return 权限列表
     */
    List<String> getUserPermissions(Long userId);

    /**
     * 获取用户角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<String> getUserRoles(Long userId);
}