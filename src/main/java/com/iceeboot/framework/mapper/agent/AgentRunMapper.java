package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentRunDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 智能体执行记录 Mapper 接口。 */
@Mapper
public interface AgentRunMapper extends BaseMapper<AgentRunDO> {
    int addUsage(
            @Param("id") String id,
            @Param("input") long input,
            @Param("output") long output,
            @Param("known") boolean known);
}
