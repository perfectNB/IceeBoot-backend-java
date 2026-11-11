/**
 * 角色菜单关联Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
package com.iceeboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.system.RoleMenuDO;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 角色菜单关联Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<RoleMenuDO> {

    /** 锁定角色行，串行处理同一角色的授权变更，包括首次分配。 */
    @Select("SELECT id FROM t_role WHERE id=#{roleId} AND del_flag=0 FOR UPDATE")
    Long lockRole(@Param("roleId") Long roleId);

    @Select("SELECT menu_id FROM t_role_menu WHERE role_id=#{roleId} AND del_flag=0")
    List<Long> selectActiveMenuIds(@Param("roleId") Long roleId);

    @Select({"<script>SELECT id FROM t_menu WHERE del_flag=0 AND id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    List<Long> selectExistingMenuIds(@Param("ids") List<Long> ids);

    // 显式删除差集关联，避免各环境逻辑删除字段配置差异影响结果。
    @Delete({"<script>DELETE FROM t_role_menu WHERE role_id=#{roleId} AND del_flag=0",
            "AND menu_id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"})
    int deleteMenuLinks(@Param("roleId") Long roleId, @Param("ids") List<Long> ids);

    /** 一次SQL写入新增关联，避免逐行填充鉴权和依赖驱动批量重写。 */
    @Insert({"<script>INSERT INTO t_role_menu",
            "(role_id,menu_id,create_by,update_by,create_time,update_time,del_flag) VALUES",
            "<foreach collection='ids' item='id' separator=','>",
            "(#{roleId},#{id},#{actor},#{actor},#{now},#{now},0)</foreach></script>"})
    int insertMenuLinks(@Param("roleId") Long roleId, @Param("ids") List<Long> ids,
            @Param("actor") String actor, @Param("now") LocalDateTime now);
}
