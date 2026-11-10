package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentReleaseDO;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 智能体发布版本 Mapper 接口。 */
@Mapper
public interface AgentReleaseMapper extends BaseMapper<AgentReleaseDO> {
    int nextVersion(@Param("agentId") long agentId);
}
