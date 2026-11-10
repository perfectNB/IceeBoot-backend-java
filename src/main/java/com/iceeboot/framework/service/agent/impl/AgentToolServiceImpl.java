package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.dto.create.agent.AgentToolDTO;
import com.iceeboot.framework.dto.update.agent.AgentToolUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentToolDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentToolMapper;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentToolVO;

import org.springframework.stereotype.Service;

/** Tool 资源服务，复用公共 CRUD，保留独立 Mapper 和接口。 */
@Service
public class AgentToolServiceImpl
        extends AbstractAgentResourceService<
                AgentToolMapper, AgentToolDO, AgentToolDTO, AgentToolUpdateDTO, AgentToolVO>
        implements IAgentToolService {
    public AgentToolServiceImpl(
            IAgentConfigurationService configs,
            IAgentPersistenceService store,
            AgentApiConverter converter) {
        super(
                configs,
                store,
                CapabilityKind.TOOL,
                converter::capability,
                converter::toolVO,
                AgentToolUpdateDTO::getId);
    }
}
