/**
 * 角色权限关联服务实现类
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
import com.iceeboot.framework.dto.create.system.RolePermissionDTO;
import com.iceeboot.framework.entity.system.RolePermissionDO;
import com.iceeboot.framework.mapper.system.SysRolePermissionMapper;
import com.iceeboot.framework.query.system.RolePermissionQuery;
import com.iceeboot.framework.service.system.ISysRolePermissionService;
import com.iceeboot.framework.vo.system.RolePermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.utils.TokenUtils;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色权限关联服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Service
@RequiredArgsConstructor
public class SysRolePermissionServiceImpl extends ServiceImpl<SysRolePermissionMapper, RolePermissionDO> implements ISysRolePermissionService {

    private final SysRolePermissionMapper sysRolePermissionMapper;

    @Override
    public PageResult<RolePermissionVO> getRolePermissionPage(RolePermissionQuery query) {
        LambdaQueryWrapper<RolePermissionDO> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getRoleId() != null) {
            wrapper.eq(RolePermissionDO::getRoleId, query.getRoleId());
        }
        if (query.getPermissionId() != null) {
            wrapper.eq(RolePermissionDO::getPermissionId, query.getPermissionId());
        }
        
        IPage<RolePermissionDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<RolePermissionDO> result = this.page(page, wrapper);
        
        List<RolePermissionVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public RolePermissionVO getRolePermissionById(Long id) {
        RolePermissionDO rolePermission = this.getById(id);
        return rolePermission != null ? convertToVO(rolePermission) : null;
    }

    @Override
    public boolean addRolePermission(RolePermissionDTO rolePermissionDTO) {
        RolePermissionDO rolePermission = new RolePermissionDO();
        BeanUtils.copyProperties(rolePermissionDTO, rolePermission);
        return this.save(rolePermission);
    }

    @Override
    public boolean updateRolePermission(RolePermissionDTO rolePermissionDTO) {
        RolePermissionDO rolePermission = new RolePermissionDO();
        BeanUtils.copyProperties(rolePermissionDTO, rolePermission);
        return this.updateById(rolePermission);
    }

    @Override
    public boolean deleteRolePermissions(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public List<Long> getPermissionIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<RolePermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermissionDO::getRoleId, roleId)
                .eq(RolePermissionDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED);
        List<RolePermissionDO> rolePermissions = this.list(wrapper);
        return rolePermissions.stream().map(RolePermissionDO::getPermissionId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIdsByPermissionId(Long permissionId) {
        LambdaQueryWrapper<RolePermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermissionDO::getPermissionId, permissionId);
        List<RolePermissionDO> rolePermissions = this.list(wrapper);
        return rolePermissions.stream().map(RolePermissionDO::getRoleId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        replacePermissionsToRole(roleId, permissionIds);
        return true;
    }

    @Override
    @Transactional
    public boolean replacePermissionsToRole(Long roleId, List<Long> permissionIds) {
        if (roleId == null || sysRolePermissionMapper.lockRole(roleId) == null) {
            throw new IceeBootException("角色不存在或已删除");
        }
        var desired = new LinkedHashSet<>(permissionIds == null ? List.<Long>of() : permissionIds);
        if (desired.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IceeBootException("权限ID必须为正整数");
        }
        var existing = new HashSet<>(sysRolePermissionMapper.selectActivePermissionIds(roleId));
        var added = desired.stream().filter(id -> !existing.contains(id)).toList();
        var removed = existing.stream().filter(id -> !desired.contains(id)).toList();
        if (added.isEmpty() && removed.isEmpty()) return false;

        // 写入前批量校验新增资源，历史逻辑删除关联不计入当前授权。
        if (!added.isEmpty() && sysRolePermissionMapper.selectExistingPermissionIds(added).size() != added.size()) {
            throw new IceeBootException("权限不存在或已删除");
        }
        if (!removed.isEmpty()) sysRolePermissionMapper.deletePermissionLinks(roleId, removed);
        if (!added.isEmpty()) {
            String actor = Objects.toString(TokenUtils.getCurrentUserIdAsString(), "system");
            int inserted = sysRolePermissionMapper.insertPermissionLinks(roleId, added, actor, LocalDateTime.now());
            if (inserted != added.size()) throw new IceeBootException("角色权限保存失败");
        }
        return true;
    }

    @Override
    public boolean removeAllPermissionsByRoleId(Long roleId) {
        LambdaQueryWrapper<RolePermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermissionDO::getRoleId, roleId);
        return this.remove(wrapper);
    }

    /**
     * 转换为VO对象
     */
    private RolePermissionVO convertToVO(RolePermissionDO rolePermission) {
        RolePermissionVO vo = new RolePermissionVO();
        BeanUtils.copyProperties(rolePermission, vo);
        // TODO: 这里可以关联查询角色名和权限名等信息
        return vo;
    }
}