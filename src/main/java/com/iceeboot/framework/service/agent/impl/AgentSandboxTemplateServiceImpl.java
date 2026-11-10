package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.dto.create.agent.AgentSandboxTemplateDTO;
import com.iceeboot.framework.dto.update.agent.AgentSandboxTemplateUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentSandboxTemplateDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentSandboxTemplateMapper;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentSandboxTemplateVO;

import org.springframework.stereotype.Service;

/** SandboxTemplate 资源服务，复用公共 CRUD，保留独立 Mapper 和接口。 */
@Service
public class AgentSandboxTemplateServiceImpl
        extends AbstractAgentResourceService<
                AgentSandboxTemplateMapper,
                AgentSandboxTemplateDO,
                AgentSandboxTemplateDTO,
                AgentSandboxTemplateUpdateDTO,
                AgentSandboxTemplateVO>
        implements IAgentSandboxTemplateService {
    public AgentSandboxTemplateServiceImpl(
            IAgentConfigurationService configs,
            IAgentPersistenceService store,
            AgentApiConverter converter) {
        super(
                configs,
                store,
                CapabilityKind.SANDBOX,
                converter::capability,
                converter::sandboxVO,
                AgentSandboxTemplateUpdateDTO::getId);
    }
}
