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
        wrapper.eq(RolePermissionDO::getRoleId, roleId);
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
        // 先删除角色的所有权限
        removeAllPermissionsByRoleId(roleId);
        
        // 再添加新的权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<RolePermissionDO> rolePermissions = permissionIds.stream()
                    .map(permissionId -> {
                        RolePermissionDO rolePermission = new RolePermissionDO();
                        rolePermission.setRoleId(roleId);
                        rolePermission.setPermissionId(permissionId);
                        return rolePermission;
                    })
                    .collect(Collectors.toList());
            return this.saveBatch(rolePermissions);
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