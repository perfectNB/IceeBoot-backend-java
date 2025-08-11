/**
 * 系统菜单Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysMenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统菜单Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenuDO> {

    /**
     * 根据用户ID查询菜单列表
     *
     * @param userId 用户ID
     * @return 菜单列表
     */
    List<SysMenuDO> selectMenusByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID查询菜单列表
     *
     * @param roleId 角色ID
     * @return 菜单列表
     */
    List<SysMenuDO> selectMenusByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据父级ID查询子菜单列表
     *
     * @param parentId 父级ID
     * @return 菜单列表
     */
    List<SysMenuDO> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 检查菜单名称是否存在
     *
     * @param menuName 菜单名称
     * @param parentId 父级ID
     * @param menuId 菜单ID（排除自己）
     * @return 是否存在
     */
    boolean checkMenuNameUnique(@Param("menuName") String menuName, @Param("parentId") Long parentId, @Param("menuId") Long menuId);
}