package com.iceeboot.framework.service.agent;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.framework.dto.create.agent.AgentSkillDTO;
import com.iceeboot.framework.dto.update.agent.AgentSkillUpdateDTO;
import com.iceeboot.framework.entity.agent.AgentSkillDO;
import com.iceeboot.framework.vo.agent.AgentSkillVO;

import java.util.List;

/** 智能体技能管理服务。 */
public interface IAgentSkillService extends IService<AgentSkillDO> {
    List<AgentSkillVO> getResourceList();

    AgentSkillVO getResourceById(long id);

    AgentSkillVO addResource(AgentSkillDTO dto);

    AgentSkillVO updateResource(AgentSkillUpdateDTO dto);

    void deleteResource(long id);
}
