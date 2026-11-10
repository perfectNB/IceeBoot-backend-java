package com.iceeboot.framework.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.framework.dto.create.agent.AgentToolDTO;
import com.iceeboot.framework.dto.update.agent.AgentToolUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentToolDO;
import com.iceeboot.framework.vo.agent.AgentToolVO;

import java.util.List;

/** 智能体工具管理服务。 */
public interface IAgentToolService extends IService<AgentToolDO> {
    List<AgentToolVO> getResourceList();

    AgentToolVO getResourceById(long id);

    AgentToolVO addResource(AgentToolDTO dto);

    AgentToolVO updateResource(AgentToolUpdateDTO dto);

    void deleteResource(long id);
}
