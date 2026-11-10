package com.iceeboot.framework.mapper.agent;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.agent.AgentKnowledgeDO;

import org.apache.ibatis.annotations.Mapper;

/** 知识库连接 Mapper 接口。 */
@Mapper
public interface AgentKnowledgeMapper extends BaseMapper<AgentKnowledgeDO> {}
