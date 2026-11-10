package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.dto.create.agent.AgentKnowledgeDTO;
import com.iceeboot.framework.dto.update.agent.AgentKnowledgeUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentKnowledgeDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentKnowledgeMapper;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentKnowledgeVO;

import org.springframework.stereotype.Service;

/** Knowledge 资源服务，复用公共 CRUD，保留独立 Mapper 和接口。 */
@Service
public class AgentKnowledgeServiceImpl
        extends AbstractAgentResourceService<
                AgentKnowledgeMapper,
                AgentKnowledgeDO,
                AgentKnowledgeDTO,
                AgentKnowledgeUpdateDTO,
                AgentKnowledgeVO>
        implements IAgentKnowledgeService {
    public AgentKnowledgeServiceImpl(
            IAgentConfigurationService configs,
            IAgentPersistenceService store,
            AgentApiConverter converter) {
        super(
                configs,
                store,
                CapabilityKind.KNOWLEDGE,
                converter::capability,
                converter::knowledgeVO,
                AgentKnowledgeUpdateDTO::getId);
    }
}
