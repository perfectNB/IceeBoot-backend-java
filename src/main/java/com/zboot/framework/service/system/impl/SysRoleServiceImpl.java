/**
 * 系统角色服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.constant.ResponseMessage;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.exception.ZbootException;
import com.zboot.framework.dto.system.RoleDTO;
import com.zboot.framework.entity.system.RoleDO;
import com.zboot.framework.entity.system.UserRoleDO;
import com.zboot.framework.mapper.system.SysRoleMapper;
import com.zboot.framework.query.system.SysRoleQuery;
import com.zboot.framework.service.system.ISysRoleService;
import com.zboot.framework.service.system.ISysRolePermissionService;
import com.zboot.framework.service.system.ISysPermissionService;
import com.zboot.framework.service.system.ISysUserRoleService;
import com.zboot.framework.vo.system.SysRoleVO;
import com.zboot.framework.vo.system.SysPermissionVO;
import com.zboot.common.constant.CacheConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, RoleDO> implements ISysRoleService {

    @Autowired
    private ISysRolePermissionService rolePermissionService;
    
    @Autowired
    private ISysPermissionService permissionService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ISysUserRoleService userRoleService;
    @Override
    public PageResult<SysRoleVO> getRolePage(SysRoleQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), RoleDO::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), RoleDO::getCode, query.getCode())
                .eq(StringUtils.hasText(query.getStatus()), RoleDO::getStatus, query.getStatus())
                .ge(query.getStartTime() != null&&!query.getStartTime().equals(""), RoleDO::getCreateTime, query.getStartTime())
                .orderByAsc(RoleDO::getSort)
                .orderByDesc(RoleDO::getCreateTime);

        // 分页查询
        IPage<RoleDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<RoleDO> result = this.page(page, wrapper);

        // 转换为VO
        List<SysRoleVO> voList = result.getRecords().stream().map(role -> {
            SysRoleVO vo = new SysRoleVO();
            BeanUtils.copyProperties(role, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public List<SysRoleVO> getAllRoles() {
        List<RoleDO> roles = this.list(
                new LambdaQueryWrapper<RoleDO>()
                        .eq(RoleDO::getStatus, "active")
                        .orderByAsc(RoleDO::getSort)
        );

        return roles.stream().map(role -> {
            SysRoleVO vo = new SysRoleVO();
            BeanUtils.copyProperties(role, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SysRoleVO getRoleById(Long id) {
        RoleDO role = this.getById(id);
        if (role == null) {
            return null;
        }

        SysRoleVO vo = new SysRoleVO();
        BeanUtils.copyProperties(role, vo);

        // 获取权限树（包含权限拥有状态）
        List<SysPermissionVO> permissionTree = permissionService.getPermissionTreeByRoleId(id);
        vo.setPermissionTree(permissionTree);
        
        return vo;
    }

    @Override
    public boolean addRole(RoleDTO roleDTO) {
        // 检查角色名称是否重复
        if (existsName(roleDTO.getName(), null)) {
            throw new ZbootException(ResponseMessage.ROLE_NAME_EXISTS);
        }

        // 检查角色编码是否重复
        if (existsCode(roleDTO.getCode(), null)) {
            throw new ZbootException(ResponseMessage.ROLE_CODE_EXISTS);
        }

        RoleDO role = new RoleDO();
        BeanUtils.copyProperties(roleDTO, role);
        return this.save(role);
    }

    @Override
    public boolean updateRole(RoleDTO roleDTO) {
        // 检查角色名称是否重复
        if (existsName(roleDTO.getName(), roleDTO.getId())) {
            throw new ZbootException(ResponseMessage.ROLE_NAME_EXISTS);
        }

        // 检查角色编码是否重复
        if (existsCode(roleDTO.getCode(), roleDTO.getId())) {
            throw new ZbootException(ResponseMessage.ROLE_CODE_EXISTS);
        }

        RoleDO role = new RoleDO();
        BeanUtils.copyProperties(roleDTO, role);
        return this.updateById(role);
    }

    @Override
    public boolean deleteRoles(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        RoleDO role = new RoleDO();
        role.setId(id);
        role.setStatus(status);
        return this.updateById(role);
    }

    @Override
    public RoleDO getRoleByCode(String code) {
        return this.getOne(
                new LambdaQueryWrapper<RoleDO>()
                        .eq(RoleDO::getCode, code)
        );
    }

    @Override
    public boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDO::getCode, code);
        if (excludeId != null) {
            wrapper.ne(RoleDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleDO::getName, name);
        if (excludeId != null) {
            wrapper.ne(RoleDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<RoleDO> getRolesByUserId(Long userId) {
        // 通过用户角色关联表查询用户的角色ID列表
        List<Long> roleIds = userRoleService.getRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        
        // 根据角色ID列表查询角色详细信息
        return this.listByIds(roleIds);
    }

    @Override
    @Transactional
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        try {
            // 先删除用户现有的所有角色
            LambdaQueryWrapper<UserRoleDO> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(UserRoleDO::getUserId, userId);
            userRoleService.remove(deleteWrapper);
            
            // 添加新的角色关联
            if (roleIds != null && !roleIds.isEmpty()) {
                List<UserRoleDO> userRoles = roleIds.stream().map(roleId -> {
                    UserRoleDO userRole = new UserRoleDO();
                    userRole.setUserId(userId);
                    userRole.setRoleId(roleId);
                    return userRole;
                }).collect(Collectors.toList());
                
                userRoleService.saveBatch(userRoles);
            }
            
            return true;
        } catch (Exception e) {
            throw new RuntimeException("分配用户角色失败", e);
        }
    }

    @Override
    @Transactional
    public boolean saveRolePermissions(Long roleId, List<Long> permissionIds) {
        try {
            // 1. 删除角色现有的所有权限
            rolePermissionService.removeAllPermissionsByRoleId(roleId);
            
            // 2. 添加新的权限
            if (permissionIds != null && !permissionIds.isEmpty()) {
                rolePermissionService.assignPermissionsToRole(roleId, permissionIds);
            }
            
            // 3. 清空缓存
            redisTemplate.delete(CacheConstants.Auth.ALL_ROLE_PERMISSIONS_KEY);
            
            return true;
        } catch (Exception e) {
            throw new RuntimeException("保存角色权限失败", e);
        }
    }
}