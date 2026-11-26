package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentModelCallDO;

import org.apache.ibatis.annotations.Mapper;

/** 模型调用记录 Mapper 接口。 */
@Mapper
public interface AgentModelCallMapper extends BaseMapper<AgentModelCallDO> {
    java.util.List<com.iceeboot.framework.vo.agent.UsageOverviewVO.Row> summarize(
            @org.apache.ibatis.annotations.Param("start") java.time.LocalDateTime start,
            @org.apache.ibatis.annotations.Param("end") java.time.LocalDateTime end,
            @org.apache.ibatis.annotations.Param("daily") boolean daily);
}
