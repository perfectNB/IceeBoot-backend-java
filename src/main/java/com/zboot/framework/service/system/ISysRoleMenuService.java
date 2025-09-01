/**
 * 角色菜单关联服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.zboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zboot.common.core.domain.PageResult;
import com.zboot.framework.entity.system.RoleMenuDO;
import com.zboot.framework.dto.system.RoleMenuDTO;
import com.zboot.framework.query.system.RoleMenuQuery;
import com.zboot.framework.vo.system.RoleMenuVO;

import java.util.List;

/**
 * 角色菜单关联服务接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
public interface ISysRoleMenuService extends IService<RoleMenuDO> {

    /**
     * 分页查询角色菜单关联列表
     *
     * @param query 查询条件
     * @return 角色菜单关联分页列表
     */
    PageResult<RoleMenuVO> getRoleMenuPage(RoleMenuQuery query);

    /**
     * 根据ID获取角色菜单关联详情
     *
     * @param id 角色菜单关联ID
     * @return 角色菜单关联详情
     */
    RoleMenuVO getRoleMenuById(Long id);

    /**
     * 新增角色菜单关联
     *
     * @param roleMenuDTO 角色菜单关联信息
     * @return 是否成功
     */
    boolean addRoleMenu(RoleMenuDTO roleMenuDTO);

    /**
     * 修改角色菜单关联
     *
     * @param roleMenuDTO 角色菜单关联信息
     * @return 是否成功
     */
    boolean updateRoleMenu(RoleMenuDTO roleMenuDTO);

    /**
     * 删除角色菜单关联
     *
     * @param ids 角色菜单关联ID列表
     * @return 是否成功
     */
    boolean deleteRoleMenus(List<Long> ids);

    /**
     * 根据角色ID获取菜单ID列表
     *
     * @param roleId 角色ID
     * @return 菜单ID列表
     */
    List<Long> getMenuIdsByRoleId(Long roleId);

    /**
     * 根据菜单ID获取角色ID列表
     *
     * @param menuId 菜单ID
     * @return 角色ID列表
     */
    List<Long> getRoleIdsByMenuId(Long menuId);

    /**
     * 为角色分配菜单
     *
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * @return 是否成功
     */
    boolean assignMenusToRole(Long roleId, List<Long> menuIds);

    /**
     * 移除角色的所有菜单
     *
     * @param roleId 角色ID
     * @return 是否成功
     */
    boolean removeAllMenusByRoleId(Long roleId);
}