/**
 * 系统用户服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.service.impl.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.SysUserDO;
import com.zboot.framework.mapper.system.SysUserMapper;
import com.zboot.framework.service.system.ISysUserService;
import com.zboot.framework.dto.system.SysUserDTO;
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
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUserDO> implements ISysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUserVO> getUserPage(SysUserQuery query) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(query.getUsername())) {
            wrapper.like(SysUserDO::getUsername, query.getUsername());
        }
        if (StringUtils.hasText(query.getNickname())) {
            wrapper.like(SysUserDO::getNickname, query.getNickname());
        }
        if (StringUtils.hasText(query.getEmail())) {
            wrapper.like(SysUserDO::getEmail, query.getEmail());
        }
        if (StringUtils.hasText(query.getPhone())) {
            wrapper.like(SysUserDO::getPhone, query.getPhone());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(SysUserDO::getStatus, query.getStatus());
        }
        
        wrapper.orderByDesc(SysUserDO::getCreateTime);
        
        Page<SysUserDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysUserDO> pageResult = this.page(page, wrapper);
        
        List<SysUserVO> voList = pageResult.getRecords().stream().map(this::convertToVO).collect(Collectors.toList());
        
        return new PageResult<>(voList, pageResult.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public SysUserVO getUserById(Long id) {
        SysUserDO user = this.getById(id);
        if (user == null) {
            return null;
        }
        return convertToVO(user);
    }

    @Override
    public boolean addUser(SysUserDTO userDTO) {
        SysUserDO user = new SysUserDO();
        BeanUtils.copyProperties(userDTO, user);
        
        // 密码加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        
        return this.save(user);
    }

    @Override
    public boolean updateUser(SysUserDTO userDTO) {
        SysUserDO user = new SysUserDO();
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
        SysUserDO user = new SysUserDO();
        user.setId(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        return this.updateById(user);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        SysUserDO user = new SysUserDO();
        user.setId(id);
        user.setStatus(status);
        return this.updateById(user);
    }

    @Override
    public SysUserDO getUserByUsername(String username) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public SysUserDO getUserByPhone(String phone) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getPhone, phone);
        return this.getOne(wrapper);
    }

    @Override
    public SysUserDO getUserByEmail(String email) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getEmail, email);
        return this.getOne(wrapper);
    }

    @Override
    public boolean existsUsername(String username, Long excludeId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(SysUserDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsPhone(String phone, Long excludeId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getPhone, phone);
        if (excludeId != null) {
            wrapper.ne(SysUserDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsEmail(String email, Long excludeId) {
        LambdaQueryWrapper<SysUserDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserDO::getEmail, email);
        if (excludeId != null) {
            wrapper.ne(SysUserDO::getId, excludeId);
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
    private SysUserVO convertToVO(SysUserDO user) {
        SysUserVO vo = new SysUserVO();
        BeanUtils.copyProperties(user, vo);
        
        // 获取用户角色和权限
        vo.setRoles(getUserRoles(user.getId()));
        vo.setPermissions(getUserPermissions(user.getId()));
        
        return vo;
    }
}