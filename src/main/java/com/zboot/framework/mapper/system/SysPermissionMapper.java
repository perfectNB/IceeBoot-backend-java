/**
 * 系统权限Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zboot.framework.entity.system.SysPermissionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统权限Mapper接口
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermissionDO> {



    /**
     * 根据父级ID查询子权限列表
     *
     * @param parentId 父级ID
     * @return 权限列表
     */
    List<SysPermissionDO> selectByParentId(@Param("parentId") Long parentId);
}