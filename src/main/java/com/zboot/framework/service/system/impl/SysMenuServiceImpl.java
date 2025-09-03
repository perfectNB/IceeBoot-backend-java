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
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.core.domain.PageResult;
import com.zboot.common.exception.ZbootException;
import com.zboot.common.utils.TokenUtils;
import com.zboot.framework.dto.system.MenuDTO;
import com.zboot.framework.entity.system.MenuDO;
import com.zboot.framework.mapper.system.SysMenuMapper;
import com.zboot.framework.dto.system.SysMenuQueryDTO;
import com.zboot.framework.service.system.IAuthCacheService;
import com.zboot.framework.service.system.ISysMenuService;
import com.zboot.framework.vo.system.SysMenuVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统菜单服务实现类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, MenuDO> implements ISysMenuService {

    @Autowired
    private IAuthCacheService authCacheService;
    @Override
    public PageResult<SysMenuVO> getMenuPage(SysMenuQueryDTO query) {
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
    public List<SysMenuVO> getUserMenu() {
        Long userId = TokenUtils.getCurrentUserId();
        // 使用缓存服务获取用户菜单
        List<Map<String, Object>> userMenus = authCacheService.getUserMenus(userId);
        
        // 将Map类型转换为SysMenuVO类型
        return userMenus.stream().map(this::convertMapToMenuVO).collect(Collectors.toList());
    }
    
    /**
     * 将Map类型的菜单数据转换为SysMenuVO
     */
    private SysMenuVO convertMapToMenuVO(Map<String, Object> menuMap) {
        SysMenuVO vo = new SysMenuVO();
        vo.setId(getLongValue(menuMap, "id"));
        vo.setTitle((String) menuMap.get("title"));
        vo.setName((String) menuMap.get("name"));
        vo.setPath((String) menuMap.get("path"));
        vo.setComponent((String) menuMap.get("component"));
        vo.setIcon((String) menuMap.get("icon"));
        vo.setParentId(getLongValue(menuMap, "parentId"));
        vo.setSort(getIntegerValue(menuMap, "sort"));
        vo.setType((String) menuMap.get("type"));
        vo.setHidden(getBooleanValue(menuMap, "hidden"));
        vo.setKeepAlive(getBooleanValue(menuMap, "keepAlive"));
        vo.setRequiresAuth(getBooleanValue(menuMap, "requiresAuth"));
        vo.setStatus((String) menuMap.get("status"));
        vo.setLink((String) menuMap.get("link"));
        
        // 处理子菜单
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> children = (List<Map<String, Object>>) menuMap.get("children");
        if (children != null && !children.isEmpty()) {
            List<SysMenuVO> childrenVOs = children.stream()
                    .map(this::convertMapToMenuVO)
                    .collect(Collectors.toList());
            vo.setChildren(childrenVOs);
        }
        
        return vo;
    }
    
    /**
     * 安全获取Long类型值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 安全获取Integer类型值
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    /**
     * 安全获取Boolean类型值
     */
    private Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    @Override
    public List<SysMenuVO> getMenuTree() {
        List<MenuDO> menus = this.list(
                new LambdaQueryWrapper<MenuDO>()
                        .eq(MenuDO::getStatus, SystemConstants.Status.ACTIVE)
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
    public boolean addMenu(MenuDTO menuDTO) {
        // 检查菜单名称是否重复
        if (existsName(menuDTO.getName(), null)) {
            throw new ZbootException(ResponseMessage.MENU_NAME_EXISTS);
        }

        MenuDO menu = new MenuDO();
        BeanUtils.copyProperties(menuDTO, menu);
        return this.save(menu);
    }

    @Override
    public boolean updateMenu(MenuDTO menuDTO) {
        // 检查菜单名称是否重复
        if (existsName(menuDTO.getName(), menuDTO.getId())) {
            throw new ZbootException(ResponseMessage.MENU_NAME_EXISTS);
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
                        .eq(MenuDO::getStatus, SystemConstants.Status.ACTIVE)
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
     * 将传入的全部菜单列表按照父子关系转换为菜单树返回
     */
    private List<SysMenuVO> buildMenuTree(List<MenuDO> menus, Long parentId) {
        if (menus == null || menus.isEmpty()) {
            return new ArrayList<>();
        }
        
        return menus.stream()
                .filter(menu -> {
                    // 如果parentId为null或0，则查找根节点（parentId为null或0的菜单）
                    if (parentId == null || parentId.equals(0L)) {
                        return menu.getParentId() == null || menu.getParentId().equals(0L);
                    }
                    // 否则查找指定父节点的子菜单
                    return parentId.equals(menu.getParentId());
                })
                .map(menu -> {
                    SysMenuVO vo = new SysMenuVO();
                    BeanUtils.copyProperties(menu, vo);
                    
                    // 递归获取子菜单
                    List<SysMenuVO> children = buildMenuTree(menus, menu.getId());
                    vo.setChildren(children);
                    
                    return vo;
                })
                .sorted((a, b) -> {
                    // 按照sort字段排序，如果sort为null则放到最后
                    if (a.getSort() == null && b.getSort() == null) return 0;
                    if (a.getSort() == null) return 1;
                    if (b.getSort() == null) return -1;
                    return a.getSort().compareTo(b.getSort());
                })
                .collect(Collectors.toList());
    }
}