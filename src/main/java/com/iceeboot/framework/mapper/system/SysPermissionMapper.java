/**
 * 系统权限Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.system.PermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统权限Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<PermissionDO> {

    /** 单次查询当前有效授权，避免旧角色资源缓存回填覆盖撤权结果。 */
    @Select("""
            SELECT p.*
            FROM t_permission p
            WHERE p.status = 'active' AND p.del_flag = 0
              AND EXISTS (
                SELECT 1
                FROM t_user_role ur
                INNER JOIN t_role r ON r.id = ur.role_id
                WHERE ur.user_id = #{userId}
                  AND ur.del_flag = 0 AND r.status = 'active' AND r.del_flag = 0
                  AND (r.code = #{superAdminCode} OR EXISTS (
                    SELECT 1
                    FROM t_role_permission rp
                    WHERE rp.role_id = r.id
                      AND rp.permission_id = p.id AND rp.del_flag = 0
                  ))
              )
            ORDER BY p.sort, p.id
            """)
    @Options(useCache = false)
    List<PermissionDO> selectAuthorizedByUserId(@Param("userId") Long userId,
                                            @Param("superAdminCode") String superAdminCode);
}
