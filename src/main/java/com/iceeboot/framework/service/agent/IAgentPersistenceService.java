package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.model.agent.*;

import java.util.List;

/** 智能体持久化服务，统一协调资源实体、发布版本和执行账本。 */
public interface IAgentPersistenceService {
    String json(Object value);

    <T> T read(String value, Class<T> type);

    List<ModelConfig> models();

    ModelConfig model(long id);

    long saveModel(ModelConfig model);

    void deleteModel(long id);

    List<Capability> capabilities();

    List<Capability> capabilities(CapabilityKind kind);

    Capability capability(long id);

    long saveCapability(Capability capability);

    void deleteCapability(long id);

    List<AgentView> agents();

    AgentView agent(long id);

    AgentView agentByCode(String code);

    long saveAgent(AgentSpec agent);

    void deleteAgent(long id);

    void lockAgent(long id);

    int nextVersion(long id);

    void publish(PublishedAgent agent, String user);

    PublishedAgent release(String id);

    List<ReleaseView> releases(long agent);

    void startRun(
            String id, String root, String parent, PublishedAgent agent, String user, String input);

    void finishRun(String id, String status, String output, String error);

    void modelCall(
            String id,
            String run,
            String root,
            String agent,
            String model,
            String input,
            String output,
            long in,
            long out,
            boolean known);

    List<RunView> runs();

    RunDetail runDetail(String root);

    boolean requestCancellation(String id);

    boolean cancellationRequested(String id);

    void heartbeat(String id);

    void diagnose(String id, String code, String trace);

    int interruptStale(long seconds);
}
