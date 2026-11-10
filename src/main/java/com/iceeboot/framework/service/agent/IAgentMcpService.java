package com.iceeboot.framework.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.framework.dto.create.agent.AgentMcpDTO;
import com.iceeboot.framework.dto.update.agent.AgentMcpUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentMcpDO;
import com.iceeboot.framework.vo.agent.AgentMcpVO;

import java.util.List;

/** 智能体MCP 服务管理服务。 */
public interface IAgentMcpService extends IService<AgentMcpDO> {
    List<AgentMcpVO> getResourceList();

    AgentMcpVO getResourceById(long id);

    AgentMcpVO addResource(AgentMcpDTO dto);

    AgentMcpVO updateResource(AgentMcpUpdateDTO dto);

    void deleteResource(long id);
}
