package com.iceeboot.framework.service.agent;
import com.iceeboot.framework.vo.agent.AgentRecoveryVO;
public interface IAgentRecoveryService {
    AgentRecoveryVO prepare(long agent,String user,String session,String run);
}
