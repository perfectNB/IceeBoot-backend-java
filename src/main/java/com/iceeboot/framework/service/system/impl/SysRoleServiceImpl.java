/**
 * 系统角色服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.CacheConstants;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.system.RoleDTO;
import com.iceeboot.framework.dto.update.system.RoleUpdateDTO;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserRoleDO;
import com.iceeboot.framework.mapper.system.SysRoleMapper;
import com.iceeboot.framework.query.system.SysRoleQuery;
import com.iceeboot.framework.service.system.*;
import com.iceeboot.framework.vo.system.SysRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统角色服务实现类
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, RoleDO> implements ISysRoleService {

    private final ISysRolePermissionService rolePermissionService;

    private final ISysPermissionService permissionService;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ISysUserRoleService userRoleService;

    private final ISysMenuService menuService;

    private final ISysRoleMenuService roleMenuService;

    private final TransactionTemplate transactionTemplate;


    @Override
    public PageResult<SysRoleVO> getRolePage(SysRoleQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<RoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), RoleDO::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), RoleDO::getCode, query.getCode())
                .eq(StringUtils.hasText(query.getStatus()), RoleDO::getStatus, query.getStatus())
                .ge(query.getStartTime() != null && !query.getStartTime().equals(""), RoleDO::getCreateTime, query.getStartTime())
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
                        .eq(RoleDO::getStatus, SystemConstants.Status.ACTIVE)
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
        return getRoleById(id, "both");
    }

    @Override
    public SysRoleVO getRoleById(Long id, String include) {
        if (include == null || !List.of("both", "permissions", "menus").contains(include)) {
            throw new IceeBootException("include只支持both、permissions或menus");
        }
        RoleDO role = this.getById(id);
        if (role == null) {
            return null;
        }

        SysRoleVO vo = new SysRoleVO();
        BeanUtils.copyProperties(role, vo);

        if (!"menus".equals(include)) {
            vo.setPermissionTree(permissionService.getPermissionTreeByRoleId(id));
        }
        if (!"permissions".equals(include)) {
            vo.setMenuTree(menuService.getMenuTreeByRoleId(id));
        }

        return vo;
    }

    @Override
    public boolean addRole(RoleDTO roleDTO) {
        // 检查角色名称是否重复
        if (existsName(roleDTO.getName(), null)) {
            throw new IceeBootException(ResponseMessage.Role.ROLE_NAME_EXISTS);
        }

        // 检查角色编码是否重复
        if (existsCode(roleDTO.getCode(), null)) {
            throw new IceeBootException(ResponseMessage.Role.ROLE_CODE_EXISTS);
        }

        RoleDO role = new RoleDO();
        BeanUtils.copyProperties(roleDTO, role);
        return this.save(role);
    }

    @Override
    public boolean updateRole(RoleUpdateDTO roleDTO) {
        // 检查角色名称是否重复
        if (existsName(roleDTO.getName(), roleDTO.getId())) {
            throw new IceeBootException(ResponseMessage.Role.ROLE_NAME_EXISTS);
        }

        // 检查角色编码是否重复
        if (existsCode(roleDTO.getCode(), roleDTO.getId())) {
            throw new IceeBootException(ResponseMessage.Role.ROLE_CODE_EXISTS);
        }

        RoleDO role = new RoleDO();
        BeanUtils.copyProperties(roleDTO, role);
        return this.updateById(role);
    }

    @Override
    public boolean deleteRoles(List<Long> ids) {
        // 检查是否有用户使用这些角色
        for (Long id : ids) {
            List<Long> userIds = userRoleService.getUserIdsByRoleId(id);
            if (!userIds.isEmpty()) {
                String userIdStr = userIds.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                throw new IceeBootException("角色删除失败，以下用户正在使用此角色，用户ID：" + userIdStr);
            }
        }

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
    public boolean saveRolePermissions(Long roleId, List<Long> permissionIds) {
        transactionTemplate.executeWithoutResult(status -> {
            if (rolePermissionService.replacePermissionsToRole(roleId, permissionIds)) {
                invalidateRoleCacheAfterCommit(CacheConstants.Auth.ROLE_PERMISSIONS_KEY + roleId);
            }
        });
        return true;
    }

    @Override
    public boolean saveRoleMenus(Long roleId, List<Long> menuIds) {
        transactionTemplate.executeWithoutResult(status -> {
            if (roleMenuService.replaceMenusToRole(roleId, menuIds)) {
                invalidateRoleCacheAfterCommit(CacheConstants.Auth.ROLE_MENUS_KEY + roleId);
            }
        });
        return true;
    }

    // 提交成功才失效旧角色快照；当前授权读取以数据库为准，不重建快照或清理URL映射。
    private void invalidateRoleCacheAfterCommit(String cacheKey) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            redisTemplate.delete(cacheKey);
                        } catch (RuntimeException ignored) {
                            // 实时鉴权不读取旧快照，清理故障不能把已提交的数据库变更报成失败。
                            log.warn("角色授权已保存，旧角色缓存清理失败；实时授权查询继续生效");
                        }
                    }
                });
    }

    @Override
    public List<RoleDO> getDefaultRoles() {
        return this.list(
                new LambdaQueryWrapper<RoleDO>()
                        .eq(RoleDO::getDefaultRole, 1)
                        .eq(RoleDO::getStatus, SystemConstants.Status.ACTIVE)
                        .eq(RoleDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED)
                        .orderByAsc(RoleDO::getSort)
        );
    }
}
