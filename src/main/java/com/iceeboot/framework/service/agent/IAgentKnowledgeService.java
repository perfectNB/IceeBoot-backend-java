package com.iceeboot.framework.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.framework.dto.create.agent.AgentKnowledgeDTO;
import com.iceeboot.framework.dto.update.agent.AgentKnowledgeUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentKnowledgeDO;
import com.iceeboot.framework.vo.agent.AgentKnowledgeVO;

import java.util.List;

/** 智能体知识库连接管理服务。 */
public interface IAgentKnowledgeService extends IService<AgentKnowledgeDO> {
    List<AgentKnowledgeVO> getResourceList();

    AgentKnowledgeVO getResourceById(long id);

    AgentKnowledgeVO addResource(AgentKnowledgeDTO dto);

    AgentKnowledgeVO updateResource(AgentKnowledgeUpdateDTO dto);

    void deleteResource(long id);
}
