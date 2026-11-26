package com.iceeboot.framework.service.agent.impl;

import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.enums.agent.UsagePeriod;
import com.iceeboot.framework.vo.agent.UsageOverviewVO;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 兼容的持久化门面，只委派到明确的数据边界，不直接操作 Mapper。 */
@Service
@RequiredArgsConstructor
public class AgentPersistenceServiceImpl implements IAgentPersistenceService {
    private final IAgentModelStorageService model;
    private final IAgentResourceStorageService resource;
    private final IAgentDefinitionStorageService definition;
    private final IAgentReleaseStorageService release;
    private final IAgentRunStorageService run;
    private final AgentJsonCodec codec;

    @Override
    public String json(Object value) {
        return codec.json(value);
    }

    @Override
    public <T> T read(String value, Class<T> type) {
        return codec.read(value, type);
    }

    @Override
    public List<ModelConfig> models() {
        return model.models();
    }

    @Override
    public ModelConfig model(long id) {
        return model.model(id);
    }

    @Override
    public long saveModel(ModelConfig m) {
        return model.saveModel(m);
    }

    @Override
    public void deleteModel(long id) {
        model.deleteModel(id);
    }

    @Override
    public List<Capability> capabilities() {
        return resource.capabilities();
    }

    @Override
    public List<Capability> capabilities(CapabilityKind kind) {
        return resource.capabilities(kind);
    }

    @Override
    public Capability capability(long id) {
        return resource.capability(id);
    }

    @Override
    public long saveCapability(Capability c) {
        return resource.saveCapability(c);
    }

    @Override
    public void deleteCapability(long id) {
        resource.deleteCapability(id);
    }

    @Override
    public List<AgentView> agents() {
        return definition.agents();
    }

    @Override
    public AgentView agent(long id) {
        return definition.agent(id);
    }

    @Override
    public AgentView agentByCode(String code) {
        return definition.agentByCode(code);
    }

    @Override
    public long saveAgent(AgentSpec a) {
        return definition.saveAgent(a);
    }

    @Override
    public void deleteAgent(long id) {
        definition.deleteAgent(id);
    }

    @Override
    public void lockAgent(long id) {
        definition.lockAgent(id);
    }

    @Override
    public int nextVersion(long id) {
        return release.nextVersion(id);
    }

    @Override
    public void publish(PublishedAgent p, String user) {
        release.publish(p, user);
    }

    @Override
    public PublishedAgent release(String id) {
        return release.release(id);
    }

    @Override
    public List<ReleaseView> releases(long agent) {
        return release.releases(agent);
    }

    @Override
    public void startRun(
            String id, String root, String parent, PublishedAgent p, String user, String input) {
        run.startRun(id, root, parent, p, user, input);
    }

    @Override
    public void finishRun(String id, String status, String output, String error) {
        run.finishRun(id, status, output, error);
    }

    @Override
    public void modelCall(
            String id,
            String run,
            String root,
            String agent,
            String model,
            String input,
            String output,
            long in,
            long out,
            Long cached,
            BillingSource billing,
            boolean known) {
        this.run.modelCall(id, run, root, agent, model, input, output, in, out, cached, billing, known);
    }

    @Override
    public UsageOverviewVO usageOverview(UsagePeriod period) {
        return run.usageOverview(period);
    }

    @Override
    public List<RunView> runs() {
        return run.runs();
    }

    @Override
    public RunDetail runDetail(String root) {
        return run.runDetail(root);
    }

    @Override
    public boolean requestCancellation(String id) {
        return run.requestCancellation(id);
    }

    @Override
    public boolean cancellationRequested(String id) {
        return run.cancellationRequested(id);
    }

    @Override
    public void heartbeat(String id) {
        run.heartbeat(id);
    }

    @Override
    public void diagnose(String id, String code, String trace) {
        run.diagnose(id, code, trace);
    }

    @Override
    public int interruptStale(long seconds) {
        return run.interruptStale(seconds);
    }
}
