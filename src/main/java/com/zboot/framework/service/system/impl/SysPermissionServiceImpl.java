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
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.exception.ZbootException;
import com.zboot.framework.dto.system.PermissionDTO;
import com.zboot.framework.entity.system.PermissionDO;
import com.zboot.framework.mapper.system.SysPermissionMapper;
import com.zboot.framework.dto.system.SysPermissionQueryDTO;
import com.zboot.framework.service.system.ISysPermissionService;
import com.zboot.framework.vo.system.SysPermissionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
                        .eq(PermissionDO::getStatus, "active")
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
    public boolean updatePermission(PermissionDTO permissionDTO) {
        // 检查权限编码是否重复
        if (existsCode(permissionDTO.getCode(), permissionDTO.getId())) {
            throw new ZbootException(ResponseMessage.PERMISSION_CODE_EXISTS);
        }

        PermissionDO permission = new PermissionDO();
        BeanUtils.copyProperties(permissionDTO, permission);
        return this.updateById(permission);
    }

    @Override
    public boolean deletePermissions(List<Long> ids) {
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
                        .eq(PermissionDO::getStatus, "active")
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
     */
    private List<SysPermissionVO> buildPermissionTree(List<PermissionDO> permissions, Long parentId) {
        return permissions.stream()
                .filter(permission -> parentId.equals(permission.getParentId()))
                .map(permission -> {
                    SysPermissionVO vo = new SysPermissionVO();
                    BeanUtils.copyProperties(permission, vo);
                    
                    // 递归获取子权限
                    List<SysPermissionVO> children = buildPermissionTree(permissions, permission.getId());
                    vo.setChildren(children);
                    
                    return vo;
                })
                .collect(Collectors.toList());
    }
}