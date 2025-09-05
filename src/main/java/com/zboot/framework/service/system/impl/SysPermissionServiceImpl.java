/**
 * 系统权限服务实现类
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
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.exception.ZbootException;
import com.zboot.framework.dto.create.system.PermissionDTO;
import com.zboot.framework.dto.update.system.PermissionUpdateDTO;
import com.zboot.framework.entity.system.PermissionDO;
import com.zboot.framework.mapper.system.SysPermissionMapper;
import com.zboot.framework.dto.create.system.SysPermissionQueryDTO;
import com.zboot.framework.service.system.ISysPermissionService;
import com.zboot.framework.service.system.ISysRolePermissionService;
import com.zboot.framework.vo.system.SysPermissionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统权限服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, PermissionDO> implements ISysPermissionService {

    @Autowired
    private ISysRolePermissionService rolePermissionService;

    @Override
    public PageResult<SysPermissionVO> getPermissionPage(SysPermissionQueryDTO query) {
        // 构建查询条件
        LambdaQueryWrapper<PermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), PermissionDO::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), PermissionDO::getCode, query.getCode())
                .eq(StringUtils.hasText(query.getType()), PermissionDO::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), PermissionDO::getStatus, query.getStatus())
                .eq(query.getParentId() != null, PermissionDO::getParentId, query.getParentId())
                .ge(query.getStartTime() != null, PermissionDO::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, PermissionDO::getCreateTime, query.getEndTime())
                .orderByAsc(PermissionDO::getSort)
                .orderByDesc(PermissionDO::getCreateTime);

        // 分页查询
        IPage<PermissionDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<PermissionDO> result = this.page(page, wrapper);

        // 转换为VO
        List<SysPermissionVO> voList = result.getRecords().stream().map(permission -> {
            SysPermissionVO vo = new SysPermissionVO();
            BeanUtils.copyProperties(permission, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public List<SysPermissionVO> getPermissionTree() {
        List<PermissionDO> permissions = this.list(
                new LambdaQueryWrapper<PermissionDO>()
                        .eq(PermissionDO::getStatus, SystemConstants.Status.ACTIVE)
                        .orderByAsc(PermissionDO::getSort)
        );

        return buildPermissionTree(permissions, 0L);
    }

    @Override
    public SysPermissionVO getPermissionById(Long id) {
        PermissionDO permission = this.getById(id);
        if (permission == null) {
            return null;
        }

        SysPermissionVO vo = new SysPermissionVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }

    @Override
    public boolean addPermission(PermissionDTO permissionDTO) {
        // 检查权限编码是否重复
        if (existsCode(permissionDTO.getCode(), null)) {
            throw new ZbootException(ResponseMessage.PERMISSION_CODE_EXISTS);
        }

        PermissionDO permission = new PermissionDO();
        BeanUtils.copyProperties(permissionDTO, permission);
        return this.save(permission);
    }

    @Override
    public boolean updatePermission(PermissionUpdateDTO dto) {
        // 检查权限编码是否重复
        if (existsCode(dto.getCode(), dto.getId())) {
            throw new ZbootException(ResponseMessage.PERMISSION_CODE_EXISTS);
        }

        PermissionDO permission = new PermissionDO();
        BeanUtils.copyProperties(dto, permission);
        return this.updateById(permission);
    }

    @Override
    public boolean deletePermissions(List<Long> ids) {
        // 检查是否有子权限或角色关联
        for (Long id : ids) {
            // 检查是否有子权限
            List<PermissionDO> children = this.list(
                new LambdaQueryWrapper<PermissionDO>()
                    .eq(PermissionDO::getParentId, id)
            );
            if (!children.isEmpty()) {
                String childNames = children.stream()
                    .map(PermissionDO::getName)
                    .collect(Collectors.joining(", "));
                throw new ZbootException("权限删除失败，存在子权限：" + childNames);
            }
            
            // 检查是否有角色使用此权限
            List<Long> roleIds = rolePermissionService.getRoleIdsByPermissionId(id);
            if (!roleIds.isEmpty()) {
                // 获取角色名称
                List<String> roleNames = roleIds.stream()
                    .map(roleId -> {
                        // 这里需要调用角色服务获取角色名称，暂时返回ID
                        return "角色ID:" + roleId;
                    })
                    .collect(Collectors.toList());
                throw new ZbootException("权限删除失败，以下角色正在使用此权限：" + String.join(", ", roleNames));
            }
        }
        
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        PermissionDO permission = new PermissionDO();
        permission.setId(id);
        permission.setStatus(status);
        return this.updateById(permission);
    }

    @Override
    public PermissionDO getPermissionByCode(String code) {
        return this.getOne(
                new LambdaQueryWrapper<PermissionDO>()
                        .eq(PermissionDO::getCode, code)
        );
    }

    @Override
    public boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<PermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PermissionDO::getCode, code);
        if (excludeId != null) {
            wrapper.ne(PermissionDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<PermissionDO> getPermissionsByUserId(Long userId) {
        // 简化实现，实际应该通过用户角色权限关联查询
        return this.list();
    }

    @Override
    public List<PermissionDO> getPermissionsByRoleId(Long roleId) {
        // 简化实现，实际应该通过角色权限关联查询
        return this.list();
    }

    @Override
    public List<SysPermissionVO> getChildrenByParentId(Long parentId) {
        List<PermissionDO> permissions = this.list(
                new LambdaQueryWrapper<PermissionDO>()
                        .eq(PermissionDO::getParentId, parentId)
                        .eq(PermissionDO::getStatus, SystemConstants.Status.ACTIVE)
                        .orderByAsc(PermissionDO::getSort)
        );

        return permissions.stream().map(permission -> {
            SysPermissionVO vo = new SysPermissionVO();
            BeanUtils.copyProperties(permission, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建权限树
     * 将传入的全部权限列表按照父子关系转换为权限树返回
     */
    private List<SysPermissionVO> buildPermissionTree(List<PermissionDO> permissions, Long parentId) {
        if (permissions == null || permissions.isEmpty()) {
            return new ArrayList<>();
        }
        
        return permissions.stream()
                .filter(permission -> {
                    // 如果parentId为null或0，则查找根节点（parentId为null或0的权限）
                    if (parentId == null || parentId.equals(0L)) {
                        return permission.getParentId() == null || permission.getParentId().equals(0L);
                    }
                    // 否则查找指定父节点的子权限
                    return parentId.equals(permission.getParentId());
                })
                .map(permission -> {
                    SysPermissionVO vo = new SysPermissionVO();
                    BeanUtils.copyProperties(permission, vo);
                    
                    // 递归获取子权限
                    List<SysPermissionVO> children = buildPermissionTree(permissions, permission.getId());
                    vo.setChildren(children);
                    
                    return vo;
                })
                .sorted((a, b) -> {
                    // 按照sort字段排序，如果sort为null则按照id排序
                    if (a.getSort() == null && b.getSort() == null) {
                        return a.getId().compareTo(b.getId());
                    }
                    if (a.getSort() == null) {
                        return Long.compare(b.getId(), a.getId());
                    }
                    if (b.getSort() == null) {
                        return Long.compare(a.getId(), b.getId());
                    }
                    // 如果sort相同，则按id排序
                    int sortCompare = a.getSort().compareTo(b.getSort());
                    if (sortCompare == 0) {
                        return a.getId().compareTo(b.getId());
                    }
                    return sortCompare;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getPermissionCodesByIds(List<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }
        
        LambdaQueryWrapper<PermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PermissionDO::getId, permissionIds)
                .select(PermissionDO::getCode);
        
        List<PermissionDO> permissions = this.list(wrapper);
        return permissions.stream()
                .map(PermissionDO::getCode)
                .collect(Collectors.toList());
    }

    @Override
    public List<SysPermissionVO> getPermissionTreeByRoleId(Long roleId) {
        // 获取所有权限
        List<PermissionDO> allPermissions = this.list();
        
        // 获取角色拥有的权限ID列表
        List<Long> rolePermissionIds = rolePermissionService.getPermissionIdsByRoleId(roleId);
        
        // 转换为VO并设置权限拥有状态
        List<SysPermissionVO> permissionVOs = allPermissions.stream().map(permission -> {
            SysPermissionVO vo = new SysPermissionVO();
            BeanUtils.copyProperties(permission, vo);
            // 设置是否拥有该权限
            vo.setHasPermission(rolePermissionIds.contains(permission.getId()));
            return vo;
        }).collect(Collectors.toList());
        
        // 构建权限树
        return buildPermissionTreeWithStatus(permissionVOs, null);
    }

    /**
     * 构建带权限状态的权限树
     */
    private List<SysPermissionVO> buildPermissionTreeWithStatus(List<SysPermissionVO> permissions, Long parentId) {
        return permissions.stream()
                .filter(permission -> {
                    if (parentId == null) {
                        return permission.getParentId() == null || permission.getParentId().equals(0L);
                    }
                    return parentId.equals(permission.getParentId());
                })
                .map(permission -> {
                    // 递归获取子权限
                    List<SysPermissionVO> children = buildPermissionTreeWithStatus(permissions, permission.getId());
                    permission.setChildren(children);
                    return permission;
                })
                .collect(Collectors.toList());
    }
}