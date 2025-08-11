/**
 * 系统菜单服务实现类
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
import com.zboot.framework.dto.system.SysMenuDTO;
import com.zboot.framework.entity.system.MenuDO;
import com.zboot.framework.mapper.system.SysMenuMapper;
import com.zboot.framework.query.system.SysMenuQuery;
import com.zboot.framework.service.system.ISysMenuService;
import com.zboot.framework.vo.system.SysMenuVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统菜单服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, MenuDO> implements ISysMenuService {

    @Override
    public PageResult<SysMenuVO> getMenuPage(SysMenuQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getTitle()), MenuDO::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getName()), MenuDO::getName, query.getName())
                .eq(StringUtils.hasText(query.getType()), MenuDO::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), MenuDO::getStatus, query.getStatus())
                .eq(query.getParentId() != null, MenuDO::getParentId, query.getParentId())
                .ge(query.getStartTime() != null, MenuDO::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, MenuDO::getCreateTime, query.getEndTime())
                .orderByAsc(MenuDO::getSort)
                .orderByDesc(MenuDO::getCreateTime);

        // 分页查询
        IPage<MenuDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<MenuDO> result = this.page(page, wrapper);

        // 转换为VO
        List<SysMenuVO> voList = result.getRecords().stream().map(menu -> {
            SysMenuVO vo = new SysMenuVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(voList, result.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public List<SysMenuVO> getMenuTree() {
        List<MenuDO> menus = this.list(
                new LambdaQueryWrapper<MenuDO>()
                        .eq(MenuDO::getStatus, "active")
                        .orderByAsc(MenuDO::getSort)
        );

        return buildMenuTree(menus, 0L);
    }

    @Override
    public SysMenuVO getMenuById(Long id) {
        MenuDO menu = this.getById(id);
        if (menu == null) {
            return null;
        }

        SysMenuVO vo = new SysMenuVO();
        BeanUtils.copyProperties(menu, vo);
        return vo;
    }

    @Override
    public boolean addMenu(SysMenuDTO menuDTO) {
        // 检查菜单名称是否重复
        if (existsName(menuDTO.getName(), null)) {
            throw new RuntimeException(ResponseMessage.MENU_NAME_EXISTS);
        }

        MenuDO menu = new MenuDO();
        BeanUtils.copyProperties(menuDTO, menu);
        return this.save(menu);
    }

    @Override
    public boolean updateMenu(SysMenuDTO menuDTO) {
        // 检查菜单名称是否重复
        if (existsName(menuDTO.getName(), menuDTO.getId())) {
            throw new RuntimeException(ResponseMessage.MENU_NAME_EXISTS);
        }

        MenuDO menu = new MenuDO();
        BeanUtils.copyProperties(menuDTO, menu);
        return this.updateById(menu);
    }

    @Override
    public boolean deleteMenus(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        MenuDO menu = new MenuDO();
        menu.setId(id);
        menu.setStatus(status);
        return this.updateById(menu);
    }

    @Override
    public boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<MenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuDO::getName, name);
        if (excludeId != null) {
            wrapper.ne(MenuDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<MenuDO> getMenusByUserId(Long userId) {
        // 简化实现，实际应该通过用户角色菜单关联查询
        return this.list();
    }

    @Override
    public List<MenuDO> getMenusByRoleId(Long roleId) {
        // 简化实现，实际应该通过角色菜单关联查询
        return this.list();
    }

    @Override
    public List<SysMenuVO> getChildrenByParentId(Long parentId) {
        List<MenuDO> menus = this.list(
                new LambdaQueryWrapper<MenuDO>()
                        .eq(MenuDO::getParentId, parentId)
                        .eq(MenuDO::getStatus, "active")
                        .orderByAsc(MenuDO::getSort)
        );

        return menus.stream().map(menu -> {
            SysMenuVO vo = new SysMenuVO();
            BeanUtils.copyProperties(menu, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 构建菜单树
     */
    private List<SysMenuVO> buildMenuTree(List<MenuDO> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> parentId.equals(menu.getParentId()))
                .map(menu -> {
                    SysMenuVO vo = new SysMenuVO();
                    BeanUtils.copyProperties(menu, vo);
                    
                    // 递归获取子菜单
                    List<SysMenuVO> children = buildMenuTree(menus, menu.getId());
                    vo.setChildren(children);
                    
                    return vo;
                })
                .collect(Collectors.toList());
    }
}