/**
 * 系统菜单服务实现类
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
import com.zboot.framework.dto.system.SysMenuDTO;
import com.zboot.framework.entity.system.SysMenuDO;
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
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenuDO> implements ISysMenuService {

    @Override
    public PageResult<SysMenuVO> getMenuPage(SysMenuQuery query) {
        // 构建查询条件
        LambdaQueryWrapper<SysMenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getTitle()), SysMenuDO::getTitle, query.getTitle())
                .like(StringUtils.hasText(query.getName()), SysMenuDO::getName, query.getName())
                .eq(StringUtils.hasText(query.getType()), SysMenuDO::getType, query.getType())
                .eq(StringUtils.hasText(query.getStatus()), SysMenuDO::getStatus, query.getStatus())
                .eq(query.getParentId() != null, SysMenuDO::getParentId, query.getParentId())
                .ge(query.getStartTime() != null, SysMenuDO::getCreateTime, query.getStartTime())
                .le(query.getEndTime() != null, SysMenuDO::getCreateTime, query.getEndTime())
                .orderByAsc(SysMenuDO::getSort)
                .orderByDesc(SysMenuDO::getCreateTime);

        // 分页查询
        IPage<SysMenuDO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<SysMenuDO> result = this.page(page, wrapper);

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
        List<SysMenuDO> menus = this.list(
                new LambdaQueryWrapper<SysMenuDO>()
                        .eq(SysMenuDO::getStatus, "active")
                        .orderByAsc(SysMenuDO::getSort)
        );

        return buildMenuTree(menus, 0L);
    }

    @Override
    public SysMenuVO getMenuById(Long id) {
        SysMenuDO menu = this.getById(id);
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
            throw new RuntimeException("菜单名称已存在");
        }

        SysMenuDO menu = new SysMenuDO();
        BeanUtils.copyProperties(menuDTO, menu);
        return this.save(menu);
    }

    @Override
    public boolean updateMenu(SysMenuDTO menuDTO) {
        // 检查菜单名称是否重复
        if (existsName(menuDTO.getName(), menuDTO.getId())) {
            throw new RuntimeException("菜单名称已存在");
        }

        SysMenuDO menu = new SysMenuDO();
        BeanUtils.copyProperties(menuDTO, menu);
        return this.updateById(menu);
    }

    @Override
    public boolean deleteMenus(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, String status) {
        SysMenuDO menu = new SysMenuDO();
        menu.setId(id);
        menu.setStatus(status);
        return this.updateById(menu);
    }

    @Override
    public boolean existsName(String name, Long excludeId) {
        LambdaQueryWrapper<SysMenuDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenuDO::getName, name);
        if (excludeId != null) {
            wrapper.ne(SysMenuDO::getId, excludeId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<SysMenuDO> getMenusByUserId(Long userId) {
        // 简化实现，实际应该通过用户角色菜单关联查询
        return this.list();
    }

    @Override
    public List<SysMenuDO> getMenusByRoleId(Long roleId) {
        // 简化实现，实际应该通过角色菜单关联查询
        return this.list();
    }

    @Override
    public List<SysMenuVO> getChildrenByParentId(Long parentId) {
        List<SysMenuDO> menus = this.list(
                new LambdaQueryWrapper<SysMenuDO>()
                        .eq(SysMenuDO::getParentId, parentId)
                        .eq(SysMenuDO::getStatus, "active")
                        .orderByAsc(SysMenuDO::getSort)
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
    private List<SysMenuVO> buildMenuTree(List<SysMenuDO> menus, Long parentId) {
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