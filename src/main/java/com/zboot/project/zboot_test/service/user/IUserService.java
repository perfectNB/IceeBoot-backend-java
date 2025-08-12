package com.zboot.project.zboot_test.service.user;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.project.zboot_test.entity.user.UserDO;
import com.zboot.project.zboot_test.dto.user.UserDTO;
import com.zboot.project.zboot_test.query.user.UserQuery;
import com.zboot.common.core.domain.PageResult;

/**
 * User服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
public interface IUserService extends IService<UserDO> {

    /**
     * 分页查询User列表
     * 
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<UserDO> getUserPage(UserQuery query);

    /**
     * 创建User
     * 
     * @param userDTO User信息
     * @return User信息
     */
    UserDO createUser(UserDTO userDTO);

    /**
     * 更新User
     * 
     * @param userDTO User信息
     * @return User信息
     */
    UserDO updateUser(UserDTO userDTO);

    /**
     * 删除User
     * 
     * @param id UserID
     */
    void deleteUser(Long id);
}