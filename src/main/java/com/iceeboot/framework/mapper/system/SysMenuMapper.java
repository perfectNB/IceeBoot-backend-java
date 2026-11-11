/**
 * 系统菜单Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.system.MenuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 系统菜单Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<MenuDO> {

    /** 单次查询当前有效授权，避免旧角色资源缓存回填覆盖撤权结果。 */
    @Select("""
            SELECT m.*
            FROM t_menu m
            WHERE m.status = 'active' AND m.del_flag = 0
              AND EXISTS (
                SELECT 1
                FROM t_user_role ur
                INNER JOIN t_role r ON r.id = ur.role_id
                INNER JOIN t_user u ON u.id = ur.user_id
                WHERE ur.user_id = #{userId}
                  AND u.status = 'active' AND u.del_flag = 0
                  AND ur.del_flag = 0 AND r.status = 'active' AND r.del_flag = 0
                  AND (r.code = #{superAdminCode} OR EXISTS (
                    SELECT 1
                    FROM t_role_menu rm
                    WHERE rm.role_id = r.id
                      AND rm.menu_id = m.id AND rm.del_flag = 0
                  ))
              )
            ORDER BY m.sort, m.id
            """)
    @Options(useCache = false)
    List<MenuDO> selectAuthorizedByUserId(@Param("userId") Long userId,
                                            @Param("superAdminCode") String superAdminCode);
}
