/**
 * 用户角色关联服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.framework.entity.system.UserRoleDO;
import com.iceeboot.framework.mapper.system.SysUserRoleMapper;
import com.iceeboot.framework.service.system.ISysUserRoleService;
import com.iceeboot.framework.dto.create.system.UserRoleDTO;
import com.iceeboot.framework.query.system.UserRoleQuery;
import com.iceeboot.framework.vo.system.UserRoleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色关联服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, UserRoleDO> implements ISysUserRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public PageResult<UserRoleVO> getUserRolePage(UserRoleQuery query) {
        LambdaQueryWrapper<UserRoleDO> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getUserId() != null) {
            wrapper.eq(UserRoleDO::getUserId, query.getUserId());
        }
        if (query.getRoleId() != null) {
            wrapper.eq(UserRoleDO::getRoleId, query.getRoleId());
        }
        
        IPage<UserRoleDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<UserRoleDO> result = this.page(page, wrapper);
        
        List<UserRoleVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public UserRoleVO getUserRoleById(Long id) {
        UserRoleDO userRole = this.getById(id);
        return userRole != null ? convertToVO(userRole) : null;
    }

    @Override
    public boolean addUserRole(UserRoleDTO userRoleDTO) {
        UserRoleDO userRole = new UserRoleDO();
        BeanUtils.copyProperties(userRoleDTO, userRole);
        return this.save(userRole);
    }

    @Override
    public boolean updateUserRole(UserRoleDTO userRoleDTO) {
        UserRoleDO userRole = new UserRoleDO();
        BeanUtils.copyProperties(userRoleDTO, userRole);
        return this.updateById(userRole);
    }

    @Override
    public boolean deleteUserRoles(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        LambdaQueryWrapper<UserRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRoleDO::getUserId, userId);
        List<UserRoleDO> userRoles = this.list(wrapper);
        return userRoles.stream().map(UserRoleDO::getRoleId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getUserIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<UserRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRoleDO::getRoleId, roleId);
        List<UserRoleDO> userRoles = this.list(wrapper);
        return userRoles.stream().map(UserRoleDO::getUserId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        // 先删除用户的所有角色
        removeAllRolesByUserId(userId);
        
        // 再添加新的角色
        if (roleIds != null && !roleIds.isEmpty()) {
            List<UserRoleDO> userRoles = roleIds.stream()
                    .map(roleId -> {
                        UserRoleDO userRole = new UserRoleDO();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());
            return this.saveBatch(userRoles);
        }
        return true;
    }

    @Override
    public boolean removeAllRolesByUserId(Long userId) {
        LambdaQueryWrapper<UserRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRoleDO::getUserId, userId);
        return this.remove(wrapper);
    }

    /**
     * 转换为VO对象
     */
    private UserRoleVO convertToVO(UserRoleDO userRole) {
        UserRoleVO vo = new UserRoleVO();
        BeanUtils.copyProperties(userRole, vo);
        // TODO: 这里可以关联查询用户名和角色名等信息
        return vo;
    }
}