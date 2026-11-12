package com.iceeboot.framework.service.agent;
import com.iceeboot.framework.vo.agent.*;
public interface IAgentWorkbenchService {
    AgentPreflightVO preflight(long agent);
    AgentReleaseDiffVO diff(long agent,String from,String to);
}
