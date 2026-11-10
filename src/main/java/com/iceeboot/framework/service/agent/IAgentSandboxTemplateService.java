package com.iceeboot.framework.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.framework.dto.create.agent.AgentSandboxTemplateDTO;
import com.iceeboot.framework.dto.update.agent.AgentSandboxTemplateUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentSandboxTemplateDO;
import com.iceeboot.framework.vo.agent.AgentSandboxTemplateVO;

import java.util.List;

/** 智能体沙箱模板管理服务。 */
public interface IAgentSandboxTemplateService extends IService<AgentSandboxTemplateDO> {
    List<AgentSandboxTemplateVO> getResourceList();

    AgentSandboxTemplateVO getResourceById(long id);

    AgentSandboxTemplateVO addResource(AgentSandboxTemplateDTO dto);

    AgentSandboxTemplateVO updateResource(AgentSandboxTemplateUpdateDTO dto);

    void deleteResource(long id);
}
