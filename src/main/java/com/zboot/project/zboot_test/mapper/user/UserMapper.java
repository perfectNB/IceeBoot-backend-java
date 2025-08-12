package com.zboot.project.zboot_test.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.project.zboot_test.entity.user.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * UserMapper
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
    // TODO: 添加自定义查询方法
}