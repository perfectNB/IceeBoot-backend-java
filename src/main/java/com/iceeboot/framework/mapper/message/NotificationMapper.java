package com.iceeboot.framework.mapper.message;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.message.NotificationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface NotificationMapper extends BaseMapper<NotificationDO> {
    @Select("SELECT * FROM t_notification WHERE id = #{id} AND del_flag = 0 FOR UPDATE")
    NotificationDO selectByIdForUpdate(@Param("id") Long id);
}
