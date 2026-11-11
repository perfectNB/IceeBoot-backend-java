/**
 * 角色菜单关联服务实现类
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
import com.iceeboot.framework.dto.create.system.RoleMenuDTO;
import com.iceeboot.framework.entity.system.RoleMenuDO;
import com.iceeboot.framework.mapper.system.SysRoleMenuMapper;
import com.iceeboot.framework.query.system.RoleMenuQuery;
import com.iceeboot.framework.service.system.ISysRoleMenuService;
import com.iceeboot.framework.vo.system.RoleMenuVO;
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
 * 角色菜单关联服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl extends ServiceImpl<SysRoleMenuMapper, RoleMenuDO> implements ISysRoleMenuService {

    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public PageResult<RoleMenuVO> getRoleMenuPage(RoleMenuQuery query) {
        LambdaQueryWrapper<RoleMenuDO> wrapper = new LambdaQueryWrapper<>();
        
        if (query.getRoleId() != null) {
            wrapper.eq(RoleMenuDO::getRoleId, query.getRoleId());
        }
        if (query.getMenuId() != null) {
            wrapper.eq(RoleMenuDO::getMenuId, query.getMenuId());
        }
        
        IPage<RoleMenuDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<RoleMenuDO> result = this.page(page, wrapper);
        
        List<RoleMenuVO> voList = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(voList, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public RoleMenuVO getRoleMenuById(Long id) {
        RoleMenuDO roleMenu = this.getById(id);
        return roleMenu != null ? convertToVO(roleMenu) : null;
    }

    @Override
    public boolean addRoleMenu(RoleMenuDTO roleMenuDTO) {
        RoleMenuDO roleMenu = new RoleMenuDO();
        BeanUtils.copyProperties(roleMenuDTO, roleMenu);
        return this.save(roleMenu);
    }

    @Override
    public boolean updateRoleMenu(RoleMenuDTO roleMenuDTO) {
        RoleMenuDO roleMenu = new RoleMenuDO();
        BeanUtils.copyProperties(roleMenuDTO, roleMenu);
        return this.updateById(roleMenu);
    }

    @Override
    public boolean deleteRoleMenus(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public List<Long> getMenuIdsByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleMenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuDO::getRoleId, roleId)
                .eq(RoleMenuDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED);
        List<RoleMenuDO> roleMenus = this.list(wrapper);
        return roleMenus.stream().map(RoleMenuDO::getMenuId).collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIdsByMenuId(Long menuId) {
        LambdaQueryWrapper<RoleMenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuDO::getMenuId, menuId);
        List<RoleMenuDO> roleMenus = this.list(wrapper);
        return roleMenus.stream().map(RoleMenuDO::getRoleId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean assignMenusToRole(Long roleId, List<Long> menuIds) {
        replaceMenusToRole(roleId, menuIds);
        return true;
    }

    @Override
    @Transactional
    public boolean replaceMenusToRole(Long roleId, List<Long> menuIds) {
        if (roleId == null || sysRoleMenuMapper.lockRole(roleId) == null) {
            throw new IceeBootException("角色不存在或已删除");
        }
        var desired = new LinkedHashSet<>(menuIds == null ? List.<Long>of() : menuIds);
        if (desired.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IceeBootException("菜单ID必须为正整数");
        }
        var existing = new HashSet<>(sysRoleMenuMapper.selectActiveMenuIds(roleId));
        var added = desired.stream().filter(id -> !existing.contains(id)).toList();
        var removed = existing.stream().filter(id -> !desired.contains(id)).toList();
        if (added.isEmpty() && removed.isEmpty()) return false;

        // 写入前批量校验新增资源，历史逻辑删除关联不计入当前授权。
        if (!added.isEmpty() && sysRoleMenuMapper.selectExistingMenuIds(added).size() != added.size()) {
            throw new IceeBootException("菜单不存在或已删除");
        }
        if (!removed.isEmpty()) sysRoleMenuMapper.deleteMenuLinks(roleId, removed);
        if (!added.isEmpty()) {
            String actor = Objects.toString(TokenUtils.getCurrentUserIdAsString(), "system");
            int inserted = sysRoleMenuMapper.insertMenuLinks(roleId, added, actor, LocalDateTime.now());
            if (inserted != added.size()) throw new IceeBootException("角色菜单保存失败");
        }
        return true;
    }

    @Override
    public boolean removeAllMenusByRoleId(Long roleId) {
        LambdaQueryWrapper<RoleMenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoleMenuDO::getRoleId, roleId);
        return this.remove(wrapper);
    }

    /**
     * 转换为VO对象
     */
    private RoleMenuVO convertToVO(RoleMenuDO roleMenu) {
        RoleMenuVO vo = new RoleMenuVO();
        BeanUtils.copyProperties(roleMenu, vo);
        // TODO: 这里可以关联查询角色名和菜单名等信息
        return vo;
    }
}