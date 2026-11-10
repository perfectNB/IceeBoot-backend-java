package com.iceeboot.framework.service.agent;

import com.iceeboot.framework.enums.agent.CapabilityKind;
import com.iceeboot.framework.model.agent.*;

import java.util.List;

/** 资源注册和明细持久化边界。 */
public interface IAgentResourceStorageService {
    List<Capability> capabilities();

    List<Capability> capabilities(CapabilityKind kind);

    Capability capability(long id);

    long saveCapability(Capability c);

    void deleteCapability(long id);
}
