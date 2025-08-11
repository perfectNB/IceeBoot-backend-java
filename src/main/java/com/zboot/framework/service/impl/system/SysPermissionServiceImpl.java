/**
 * 系统权限服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.impl.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.dto.system.SysPermissionDTO;
import com.zboot.framework.entity.system.SysPermissionDO;
import com.zboot.framework.mapper.system.SysPermissionMapper;
import com.zboot.framework.query.system.SysPermissionQuery;
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
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermissionDO> implements ISysPermissionService {

    @Override
    public PageResult<SysPermissionVO> getPermissionPage(SysPermissionQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<SysPermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), SysPermissionDO::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), SysPermissionDO::getCode, query.getCode())
                .eq(StringUtils.hasText(query.getType()), SysPermissionDO::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), SysPermissionDO::getStatus, query.getStatus())
                .eq(query.getParentId() != null, SysPermissionDO::getParentId, query.getParentId())
                .ge(query.getStartTime() != null, SysPermissionDO::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, SysPermissionDO::getCreateTime, query.getEndTime())
                .orderByAsc(SysPermissionDO::getSort)
                .orderByDesc(SysPermissionDO::getCreateTime);

        // 分页查询
        IPage<SysPermissionDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysPermissionDO> result = this.page(page, wrapper);

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
        List<SysPermissionDO> permissions = this.list(
                new LambdaQueryWrapper<SysPermissionDO>()
                        .eq(SysPermissionDO::getStatus, "active")
                        .orderByAsc(SysPermissionDO::getSort)
        );

        return buildPermissionTree(permissions, 0L);
    }

    @Override
    public SysPermissionVO getPermissionById(Long id) {
        SysPermissionDO permission = this.getById(id);
        if (permission == null) {
            return null;
        }

        SysPermissionVO vo = new SysPermissionVO();
        BeanUtils.copyProperties(permission, vo);
        return vo;
    }

    @Override
    public boolean addPermission(SysPermissionDTO permissionDTO) {
        // 检查权限编码是否重复
        if (existsCode(permissionDTO.getCode(), null)) {
            throw new RuntimeException("权限编码已存在");
        }

        SysPermissionDO permission = new SysPermissionDO();
        BeanUtils.copyProperties(permissionDTO, permission);
        return this.save(permission);
    }

    @Override
    public boolean updatePermission(SysPermissionDTO permissionDTO) {
        // 检查权限编码是否重复
        if (existsCode(permissionDTO.getCode(), permissionDTO.getId())) {
            throw new RuntimeException("权限编码已存在");
        }

        SysPermissionDO permission = new SysPermissionDO();
        BeanUtils.copyProperties(permissionDTO, permission);
        return this.updateById(permission);
    }

    @Override
    public boolean deletePermissions(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        SysPermissionDO permission = new SysPermissionDO();
        permission.setId(id);
        permission.setStatus(status);
        return this.updateById(permission);
    }

    @Override
    public SysPermissionDO getPermissionByCode(String code) {
        return this.getOne(
                new LambdaQueryWrapper<SysPermissionDO>()
                        .eq(SysPermissionDO::getCode, code)
        );
    }

    @Override
    public boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<SysPermissionDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermissionDO::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysPermissionDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<SysPermissionDO> getPermissionsByUserId(Long userId) {
        // 简化实现，实际应该通过用户角色权限关联查询
        return this.list();
    }

    @Override
    public List<SysPermissionDO> getPermissionsByRoleId(Long roleId) {
        // 简化实现，实际应该通过角色权限关联查询
        return this.list();
    }

    @Override
    public List<SysPermissionVO> getChildrenByParentId(Long parentId) {
        List<SysPermissionDO> permissions = this.list(
                new LambdaQueryWrapper<SysPermissionDO>()
                        .eq(SysPermissionDO::getParentId, parentId)
                        .eq(SysPermissionDO::getStatus, "active")
                        .orderByAsc(SysPermissionDO::getSort)
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
    private List<SysPermissionVO> buildPermissionTree(List<SysPermissionDO> permissions, Long parentId) {
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