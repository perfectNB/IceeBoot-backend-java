package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.model.agent.*;

import java.util.List;

/** 智能体配置校验、密钥保护与事务发布服务。 */
public interface IAgentConfigurationService {
    List<ModelConfig> models();

    ModelConfig saveModel(ModelConfig model);

    List<Capability> capabilities();

    List<Capability> capabilities(CapabilityKind kind);

    Capability capability(long id);

    Capability saveCapability(Capability capability);

    List<AgentView> agents();

    AgentView agent(long id);

    AgentView saveAgent(AgentSpec agent);

    ReleaseView publish(long id, String user);
    PublishedAgent preview(long id);
    ReleaseView rollback(long id,String releaseId,String user);
}
