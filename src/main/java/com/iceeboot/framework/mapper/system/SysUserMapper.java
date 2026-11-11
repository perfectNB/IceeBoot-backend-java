/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserDO;
import com.iceeboot.framework.vo.system.UserRoleSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;


/**
 * 系统用户Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Mapper
public interface SysUserMapper extends BaseMapper<UserDO> {

    @Select("""
            <script>
            SELECT DISTINCT ur.user_id AS userId, r.id, r.name, r.code, r.status, r.sort
            FROM t_user_role ur INNER JOIN t_role r ON r.id = ur.role_id
            WHERE ur.del_flag = 0 AND r.del_flag = 0
              AND ur.user_id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            ORDER BY r.sort, r.id
            </script>
            """)
    List<UserRoleSummaryVO> selectRoleSummariesByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 根据用户ID查询权限列表
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    List<PermissionDO> selectPermissionsByUserId(Long userId);

    /**
     * 根据用户ID查询角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<RoleDO> selectRolesByUserId(Long userId);

    /**
     * 使用悲观锁查询用户（用于并发更新用户关联数据）
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM t_user WHERE id = #{id} FOR UPDATE")
    UserDO selectByIdForUpdate(@Param("id") Long id);

}
