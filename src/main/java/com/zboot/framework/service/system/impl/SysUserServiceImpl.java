/**
 * 系统用户服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.UserDO;
import com.zboot.framework.mapper.system.SysUserMapper;
import com.zboot.framework.service.system.ISysUserService;
import com.zboot.framework.dto.system.UserDTO;
import com.zboot.framework.query.system.SysUserQuery;
import com.zboot.framework.vo.system.SysUserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, UserDO> implements ISysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUserVO> getUserPage(SysUserQuery query) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(UserDO::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getNickname())) {
            wrapper.like(UserDO::getNickname, query.getNickname());
        }
        if (StringUtils.hasText(query.getEmail())) {
            wrapper.like(UserDO::getEmail, query.getEmail());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(UserDO::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(UserDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(UserDO::getCreateTime);
        
        Page<UserDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<UserDO> pageResult = this.page(page, wrapper);
        
        List<SysUserVO> voList = pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        
        return new PageResult<>(voList, pageResult.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public SysUserVO getUserById(Long id) {
        UserDO user = this.getById(id);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public boolean addUser(UserDTO userDTO) {
        UserDO user = new UserDO();
        BeanUtils.copyProperties(userDTO, user);
        
        // 密码加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return this.save(user);
    }

    @Override
    public boolean updateUser(UserDTO userDTO) {
        UserDO user = new UserDO();
        BeanUtils.copyProperties(userDTO, user);
        
        // 如果有新密码，进行加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return this.updateById(user);
    }

    @Override
    public boolean deleteUsers(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean resetPassword(Long id, String newPassword) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(user);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setStatus(status);
        return this.updateById(user);
    }

    @Override
    public UserDO getUserByUsername(String username) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public UserDO getUserByPhone(String phone) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone);
        return this.getOne(wrapper);
    }

    @Override
    public UserDO getUserByEmail(String email) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getEmail, email);
        return this.getOne(wrapper);
    }

    @Override
    public boolean existsUsername(String username, Long excludeId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(UserDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsPhone(String phone, Long excludeId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getPhone, phone);
        if (excludeId != null) {
            wrapper.ne(UserDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsEmail(String email, Long excludeId) {
        LambdaQueryWrapper<UserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserDO::getEmail, email);
        if (excludeId != null) {
            wrapper.ne(UserDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<String> getUserPermissions(Long userId) {
        return sysUserMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        return sysUserMapper.selectRolesByUserId(userId);
    }

    /**
     * 转换为VO对象
     */
    private SysUserVO convertToVO(UserDO user) {
        SysUserVO vo = new SysUserVO();
        BeanUtils.copyProperties(user, vo);
        
        // 获取用户角色和权限
        vo.setRoles(getUserRoles(user.getId()));
        vo.setPermissions(getUserPermissions(user.getId()));
        
        return vo;
    }
}