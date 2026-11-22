package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.dto.create.agent.AgentScheduleDTO;
import com.iceeboot.framework.dto.update.agent.AgentScheduleUpdateDTO;
import com.iceeboot.framework.vo.agent.*;
import java.util.List;

/** 只管理用户调度配置和查询，不执行模型。 */
public interface IAgentScheduleService {
    List<AgentScheduleVO> list(long owner);
    AgentScheduleVO create(long owner, AgentScheduleDTO request);
    AgentScheduleVO update(long owner, AgentScheduleUpdateDTO request);
    AgentScheduleVO enabled(long owner, long id, boolean enabled);
    void delete(long owner, long id);
    List<AgentScheduleExecutionVO> executions(long owner, long id, int offset);
}
