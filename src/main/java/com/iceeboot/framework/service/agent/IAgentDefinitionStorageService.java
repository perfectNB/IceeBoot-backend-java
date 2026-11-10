package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.model.agent.*;

import java.util.List;

/** 智能体定义持久化边界。 */
public interface IAgentDefinitionStorageService {
    List<AgentView> agents();

    AgentView agent(long id);

    AgentView agentByCode(String code);

    long saveAgent(AgentSpec a);

    void deleteAgent(long id);

    void lockAgent(long id);
}
