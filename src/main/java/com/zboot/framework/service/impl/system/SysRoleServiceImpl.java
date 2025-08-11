/**
 * 系统角色服务实现类
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
import com.zboot.framework.dto.system.SysRoleDTO;
import com.zboot.framework.entity.system.SysRoleDO;
import com.zboot.framework.mapper.system.SysRoleMapper;
import com.zboot.framework.query.system.SysRoleQuery;
import com.zboot.framework.service.system.ISysRoleService;
import com.zboot.framework.vo.system.SysRoleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
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
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRoleDO> implements ISysRoleService {

    @Override
    public PageResult<SysRoleVO> getRolePage(SysRoleQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getName()), SysRoleDO::getName, query.getName())
                .like(StringUtils.hasText(query.getCode()), SysRoleDO::getCode, query.getCode())
                .eq(StringUtils.hasText(query.getStatus()), SysRoleDO::getStatus, query.getStatus())
                .ge(query.getStartTime() != null, SysRoleDO::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, SysRoleDO::getCreateTime, query.getEndTime())
                .orderByAsc(SysRoleDO::getSort)
                .orderByDesc(SysRoleDO::getCreateTime);

        // 分页查询
        IPage<SysRoleDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysRoleDO> result = this.page(page, wrapper);

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
        List<SysRoleDO> roles = this.list(
                new LambdaQueryWrapper<SysRoleDO>()
                        .eq(SysRoleDO::getStatus, "active")
                        .orderByAsc(SysRoleDO::getSort)
        );

        return roles.stream().map(role -> {
            SysRoleVO vo = new SysRoleVO();
            BeanUtils.copyProperties(role, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public SysRoleVO getRoleById(Long id) {
        SysRoleDO role = this.getById(id);
        if (role == null) {
            return null;
        }

        SysRoleVO vo = new SysRoleVO();
        BeanUtils.copyProperties(role, vo);
        return vo;
    }

    @Override
    public boolean addRole(SysRoleDTO roleDTO) {
        // 检查角色名称是否重复
        if (existsName(roleDTO.getName(), null)) {
            throw new RuntimeException("角色名称已存在");
        }

        // 检查角色编码是否重复
        if (existsCode(roleDTO.getCode(), null)) {
            throw new RuntimeException("角色编码已存在");
        }

        SysRoleDO role = new SysRoleDO();
        BeanUtils.copyProperties(roleDTO, role);
        return this.save(role);
    }

    @Override
    public boolean updateRole(SysRoleDTO roleDTO) {
        // 检查角色名称是否重复
        if (existsName(roleDTO.getName(), roleDTO.getId())) {
            throw new RuntimeException("角色名称已存在");
        }

        // 检查角色编码是否重复
        if (existsCode(roleDTO.getCode(), roleDTO.getId())) {
            throw new RuntimeException("角色编码已存在");
        }

        SysRoleDO role = new SysRoleDO();
        BeanUtils.copyProperties(roleDTO, role);
        return this.updateById(role);
    }

    @Override
    public boolean deleteRoles(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        SysRoleDO role = new SysRoleDO();
        role.setId(id);
        role.setStatus(status);
        return this.updateById(role);
    }

    @Override
    public SysRoleDO getRoleByCode(String code) {
        return this.getOne(
                new LambdaQueryWrapper<SysRoleDO>()
                        .eq(SysRoleDO::getCode, code)
        );
    }

    @Override
    public boolean existsCode(String code, Long excludeId) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleDO::getCode, code);
        if (excludeId != null) {
            wrapper.ne(SysRoleDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<SysRoleDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRoleDO::getName, name);
        if (excludeId != null) {
            wrapper.ne(SysRoleDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<SysRoleDO> getRolesByUserId(Long userId) {
        // 简化实现，实际应该通过用户角色关联表查询
        return this.list();
    }

    @Override
    public boolean assignRolesToUser(Long userId, List<Long> roleIds) {
        // 简化实现，实际应该操作用户角色关联表
        return true;
    }
}