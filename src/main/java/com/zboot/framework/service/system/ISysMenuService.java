/**
 * 菜单服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.MenuDO;
import com.zboot.framework.dto.system.MenuDTO;
import com.zboot.framework.dto.system.SysMenuQueryDTO;
import com.zboot.framework.vo.system.SysMenuVO;

import java.util.List;
import java.util.Map;

/**
 * 菜单服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public interface ISysMenuService extends IService<MenuDO> {

    /**
     * 分页查询菜单列表
     *
     * @param query 查询条件
     * @return 菜单分页列表
     */
    PageResult<SysMenuVO> getMenuPage(SysMenuQueryDTO query);


    /**
     * 获取用户菜单
     *
     * @return 用户菜单列表
     */
    List<SysMenuVO> getUserMenu();

    /**
     * 获取菜单树
     *
     * @return 菜单树列表
     */
    List<SysMenuVO> getMenuTree();

    /**
     * 根据ID获取菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    SysMenuVO getMenuById(Long id);

    /**
     * 新增菜单
     *
     * @param menuDTO 菜单信息
     * @return 是否成功
     */
    boolean addMenu(MenuDTO menuDTO);

    /**
     * 更新菜单
     *
     * @param menuDTO 菜单信息
     * @return 是否成功
     */
    boolean updateMenu(MenuDTO menuDTO);

    /**
     * 删除菜单
     *
     * @param ids 菜单ID列表
     * @return 是否成功
     */
    boolean deleteMenus(List<Long> ids);

    /**
     * 修改菜单状态
     *
     * @param id 菜单ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, String status);

    /**
     * 检查菜单名称是否存在
     *
     * @param name 菜单名称
     * @param id 菜单ID（排除自己）
     * @return 是否存在
     */
    boolean existsName(String name, Long id);

    /**
     * 根据用户ID获取菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<MenuDO> getMenusByUserId(Long userId);

    /**
     * 根据角色ID获取菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    List<MenuDO> getMenusByRoleId(Long roleId);

    /**
     * 根据父级ID获取子菜单列表
     *
     * @param parentId 父级ID
     * @return 菜单列表
     */
    List<SysMenuVO> getChildrenByParentId(Long parentId);

    /**
     * 根据角色ID获取菜单树
     *
     * @param roleId 角色ID
     * @return 菜单树列表
     */
    List<SysMenuVO> getMenuTreeByRoleId(Long roleId);
}