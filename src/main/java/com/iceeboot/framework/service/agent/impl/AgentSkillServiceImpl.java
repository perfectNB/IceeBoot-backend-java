package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.dto.create.agent.AgentSkillDTO;
import com.iceeboot.framework.dto.update.agent.AgentSkillUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentSkillDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentSkillMapper;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentSkillVO;

import org.springframework.stereotype.Service;

/** Skill 资源服务，复用公共 CRUD，保留独立 Mapper 和接口。 */
@Service
public class AgentSkillServiceImpl
        extends AbstractAgentResourceService<
                AgentSkillMapper, AgentSkillDO, AgentSkillDTO, AgentSkillUpdateDTO, AgentSkillVO>
        implements IAgentSkillService {
    public AgentSkillServiceImpl(
            IAgentConfigurationService configs,
            IAgentPersistenceService store,
            AgentApiConverter converter) {
        super(
                configs,
                store,
                CapabilityKind.SKILL,
                converter::capability,
                converter::skillVO,
                AgentSkillUpdateDTO::getId);
    }
}
