package com.zboot.project.zboot_test.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.core.domain.PageResult;
import com.zboot.project.zboot_test.entity.user.UserDO;
import com.zboot.project.zboot_test.dto.user.UserDTO;
import com.zboot.project.zboot_test.mapper.user.UserMapper;
import com.zboot.project.zboot_test.query.user.UserQuery;
import com.zboot.project.zboot_test.service.user.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * User服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements IUserService {

    @Override
    public PageResult<UserDO> getUserPage(UserQuery query) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        
        // TODO: 根据实际需求添加查询条件
        // if (StringUtils.hasText(query.getName())) {
        //     wrapper.like(UserDO::getName, query.getName());
        // }
        
        wrapper.orderByDesc(UserDO::getCreateTime);
        
        Page<UserDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<UserDO> pageResult = this.page(page, wrapper);
        
        return new PageResult<>(pageResult.getRecords(), pageResult.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public UserDO createUser(UserDTO userDTO) {
        try {
            UserDO user = new UserDO();
            BeanUtils.copyProperties(userDTO, user);
            
            // TODO: 添加业务逻辑
            
            this.save(user);
            return user;
        } catch (Exception e) {
            log.error("创建User失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建User失败", e);
        }
    }

    @Override
    public UserDO updateUser(UserDTO userDTO) {
        try {
            UserDO user = this.getById(userDTO.getId());
            if (user == null) {
                throw new RuntimeException("User不存在");
            }
            
            BeanUtils.copyProperties(userDTO, user);
            
            // TODO: 添加业务逻辑
            
            this.updateById(user);
            return user;
        } catch (Exception e) {
            log.error("更新User失败: {}", e.getMessage(), e);
            throw new RuntimeException("更新User失败", e);
        }
    }

    @Override
    public void deleteUser(Long id) {
        try {
            UserDO user = this.getById(id);
            if (user != null) {
                // TODO: 添加删除前的业务逻辑检查
                
                this.removeById(id);
                log.info("删除User成功: {}", id);
            }
        } catch (Exception e) {
            log.error("删除User失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除User失败", e);
        }
    }
}