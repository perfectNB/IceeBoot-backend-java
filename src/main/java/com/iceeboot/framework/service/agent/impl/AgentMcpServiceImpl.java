package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.dto.create.agent.AgentMcpDTO;
import com.iceeboot.framework.dto.update.agent.AgentMcpUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentMcpDO;
import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.mapper.agent.AgentMcpMapper;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.vo.agent.AgentMcpVO;

import org.springframework.stereotype.Service;

/** Mcp 资源服务，复用公共 CRUD，保留独立 Mapper 和接口。 */
@Service
public class AgentMcpServiceImpl
        extends AbstractAgentResourceService<
                AgentMcpMapper, AgentMcpDO, AgentMcpDTO, AgentMcpUpdateDTO, AgentMcpVO>
        implements IAgentMcpService {
    public AgentMcpServiceImpl(
            IAgentConfigurationService configs,
            IAgentPersistenceService store,
            AgentApiConverter converter) {
        super(
                configs,
                store,
                CapabilityKind.MCP,
                converter::capability,
                converter::mcpVO,
                AgentMcpUpdateDTO::getId);
    }
}
