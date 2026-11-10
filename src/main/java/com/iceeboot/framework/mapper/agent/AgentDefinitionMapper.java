package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentDefinitionDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 智能体定义 Mapper 接口。 */
@Mapper
public interface AgentDefinitionMapper extends BaseMapper<AgentDefinitionDO> {
    java.util.List<AgentDefinitionDO> selectWithStats(
            @Param("id") Long id, @Param("code") String code);

    Long lockById(@Param("id") long id);
}
