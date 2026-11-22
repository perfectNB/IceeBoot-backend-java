package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.entity.agent.AgentScheduleExecutionDO;
import com.iceeboot.framework.enums.agent.AgentScheduleExecutionStatus;
import java.util.List;

/** 跨调度与执行记录的原子认领边界，不在事务内调用外部服务。 */
public interface IAgentScheduleDispatchService {
    List<Long> due();
    List<Long> expired();
    AgentScheduleExecutionDO claim(long id);
    void recover(long id);
    void bindRun(String id, String run);
    void finish(AgentScheduleExecutionDO execution, AgentScheduleExecutionStatus status, String message);
}
