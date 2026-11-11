package com.iceeboot.framework.service.agent.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.runtime.*;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AgentExecutionServiceImpl implements IAgentExecutionService {
    private final IAgentPersistenceService store;
    private final HarnessRuntimeFactory harness;
    private final AgentModelFactory models;
    private final AgentProperties properties;
    private final AgentRunControl control;
    private final IAgentQuotaService quota;
    private final Set<String> activeSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final ObjectMapper json;
    private final IAgentConversationService conversations;

    public AgentExecutionServiceImpl(
            IAgentPersistenceService store,
            HarnessRuntimeFactory harness,
            AgentModelFactory models,
            AgentProperties properties,
            ObjectMapper json,
            IAgentQuotaService quota,
            AgentRunControl control, IAgentConversationService conversations) {
        this.store = store;
        this.harness = harness;
        this.models = models;
        this.properties = properties;
        this.quota = quota;
        this.control = control;
        this.json = json;
        this.conversations = conversations;
    }

    @Override
    public InvokeResult invoke(String code, InvokeRequest request, Caller caller) {
        control.acquire();
        List<AutoCloseable> resources = new CopyOnWriteArrayList<>();
        RunRecorder recorder = null;
        String sessionKey = null;
        boolean sessionAcquired = false;
        com.iceeboot.framework.entity.agent.AgentSessionDO conversation = null;
        String turnId = null;
        boolean turnStarted = false;
        AgentRunControl.Registration registration = null;
        try {
            AgentView view = store.agentByCode(code);
            String session = request.sessionId() == null || request.sessionId().isBlank()
                    ? UUID.randomUUID().toString() : request.sessionId();
            conversation = conversations.open(view.draft().id(),caller.userId(),session,
                    store.release(view.activeReleaseId()),request.input());
            PublishedAgent p = store.release(conversation.getReleaseId());
            if (!p.agent().enabled())
                throw new IceeBootException(ResponseMessage.Agent.AGENT_RELEASE_DISABLED);
            AtomicInteger budget = new AtomicInteger(properties.getMaxModelRequestsPerRun());
            recorder =
                    new RunRecorder(
                            null,
                            "",
                            p,
                            caller,
                            store,
                            budget,
                            () -> quota.acquire(caller.userId()));

            sessionKey = p.agent().id() + ":" + caller.userId() + ":" + session;
            sessionAcquired = activeSessions.add(sessionKey);
            if (!sessionAcquired)
                throw new IceeBootException(
                        ResultCodeEnum.ERROR.getCode(),
                        ResponseMessage.Agent.EXECUTION_SESSION_BUSY);
            turnId = recorder.id;
            conversations.begin(conversation,turnId,request.input());
            turnStarted = true;
            List<Msg> input = List.of(new UserMessage(request.input()));
            recorder.start(input);
            registration = control.register(recorder.id);
            registration.check();
            var agent = harness.create(p, caller, recorder, budget, resources, session);
            resources.add(agent);
            registration.check();
            RuntimeContext context =
                    RuntimeContext.builder()
                            .userId(caller.userId())
                            .sessionId(session)
                            .runId(recorder.id)
                            .build();
            AgentSessionLease lease = new AgentSessionLease(agent, context);
            recorder.trackSession(lease);
            resources.add(0, lease);
            io.agentscope.core.agent.AgentRun<Msg> prepared =
                    io.agentscope.core.agent.AgentRun.create(
                            p.agent().code(),
                            recorder.id,
                            () ->
                                    p.agent().outputFormat() == OutputFormat.JSON_SCHEMA
                                            ? agent.call(
                                                    input,
                                                    json.valueToTree(p.agent().outputSchema()),
                                                    context)
                                            : agent.call(input, context));
            registration.bind(prepared::interrupt);
            Msg response =
                    prepared.stream()
                            .last()
                            .block(Duration.ofSeconds(properties.getExecutionTimeoutSeconds()));
            registration.check();
            RunDetail detail = store.runDetail(recorder.rootId);
            conversations.finish(conversation,turnId,RunRecorder.text(response),"SUCCEEDED");
            return new InvokeResult(
                    recorder.rootId, session,
                    RunRecorder.text(response),
                    detail.totalInputTokens(),
                    detail.totalOutputTokens(),
                    detail.usageKnown());
        } catch (RuntimeException e) {
            try {
                if (turnStarted) conversations.finish(conversation, turnId, "",
                        isTimeout(e) ? "TIMED_OUT"
                                : registration != null && registration.cancelled() ? "CANCELLED" : "FAILED");
            } catch (RuntimeException recordingFailure) {
                if (recordingFailure != e) e.addSuppressed(recordingFailure);
            }
            recordTermination(recorder, registration, e);
            if (registration != null && registration.cancelled())
                throw new IceeBootException(409, ResponseMessage.Agent.EXECUTION_CANCELLED, e);
            if (isTimeout(e))
                throw new IceeBootException(504, ResponseMessage.Agent.EXECUTION_TIMED_OUT, e);
            if (e instanceof IceeBootException businessException) throw businessException;
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(), ResponseMessage.Agent.EXECUTION_FAILED, e);
        } finally {
            if (registration != null) registration.close();
            List<AutoCloseable> closing = List.copyOf(resources);
            for (int i = closing.size() - 1; i >= 0; i--)
                try {
                    closing.get(i).close();
                } catch (Exception ignored) {
                    /* 终态清理继续释放其他资源，不输出凭据。 */
                }
            try {
                if (turnStarted) conversations.release(conversation,turnId);
            } catch (RuntimeException releaseFailure) {
                com.iceeboot.framework.service.agent.support.AgentDiagnostics.failure(
                        turnId, "SESSION_RELEASE", com.iceeboot.framework.service.agent.support.AgentDiagnostics.traceId(), releaseFailure);
            } finally {
                if (sessionAcquired) activeSessions.remove(sessionKey);
                control.release();
            }
        }
    }

    /** 直接模型调用使用相同的额度和账本，不装配 Agent 工具。 */
    @Override
    public String invokeModel(ModelInvokeRequest request, Caller caller) {
        control.acquire();
        RunRecorder recorder = null;
        AgentRunControl.Registration registration = null;
        try {
            ModelConfig model = store.model(request.modelConfigId());
            AgentSpec spec =
                    new AgentSpec(
                            0L,
                            "model_" + model.id(),
                            model.name(),
                            "",
                            "",
                            "",
                            model.id(),
                            1,
                            true,
                            false,
                            null,
                            false,
                            OutputFormat.TEXT,
                            Map.of(),
                            model.generation(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            Map.of());
            PublishedAgent invocation =
                    new PublishedAgent("", 0, spec, model, List.of(), List.of());
            recorder =
                    new RunRecorder(
                            null,
                            "",
                            invocation,
                            caller,
                            store,
                            new AtomicInteger(1),
                            () -> quota.acquire(caller.userId()));
            List<Msg> messages = new ArrayList<>();
            if (request.systemPrompt() != null && !request.systemPrompt().isBlank())
                messages.add(
                        Msg.builder()
                                .role(MsgRole.SYSTEM)
                                .textContent(request.systemPrompt())
                                .build());
            messages.add(new UserMessage(request.input()));
            recorder.start(messages);
            registration = control.register(recorder.id);
            registration.check();
            StringBuilder output = new StringBuilder();
            var task =
                    recorder.observe(models.create(model, null)).stream(
                                    messages, List.of(), models.options(model, null))
                            .doOnNext(
                                    response -> {
                                        if (response.getContent() != null)
                                            response.getContent().stream()
                                                    .filter(TextBlock.class::isInstance)
                                                    .map(TextBlock.class::cast)
                                                    .forEach(
                                                            block ->
                                                                    output.append(block.getText()));
                                    })
                            .then();
            registration
                    .apply(task)
                    .block(
                            Duration.ofSeconds(
                                    Math.min(120, properties.getExecutionTimeoutSeconds())));
            registration.check();
            recorder.succeed(output.toString());
            return output.toString();
        } catch (RuntimeException error) {
            recordTermination(recorder, registration, error);
            if (registration != null && registration.cancelled())
                throw new IceeBootException(409, ResponseMessage.Agent.EXECUTION_CANCELLED, error);
            if (isTimeout(error))
                throw new IceeBootException(504, ResponseMessage.Agent.EXECUTION_TIMED_OUT, error);
            if (error instanceof IceeBootException business) throw business;
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.MODEL_INVOKE_FAILED,
                    error);
        } finally {
            if (registration != null) registration.close();
            control.release();
        }
    }

    private void recordTermination(
            RunRecorder recorder,
            AgentRunControl.Registration registration,
            RuntimeException error) {
        if (recorder == null) return;
        try {
            if (registration != null && registration.cancelled()) {
                recorder.finish("CANCELLED", ResponseMessage.Agent.EXECUTION_CANCELLED);
                store.finishRun(
                        recorder.id, "CANCELLED", "", ResponseMessage.Agent.EXECUTION_CANCELLED);
            } else if (isTimeout(error)) {
                com.iceeboot.framework.service.agent.support.AgentDiagnostics.failure(
                        recorder.id, "TIMEOUT", recorder.traceId(), error);
                store.diagnose(recorder.id, "TIMEOUT", recorder.traceId());
                store.finishRun(
                        recorder.id, "TIMED_OUT", "", ResponseMessage.Agent.EXECUTION_TIMED_OUT);
            } else recorder.fail(error);
        } catch (RuntimeException recordingFailure) {
            if (recordingFailure != error) error.addSuppressed(recordingFailure);
        }
    }

    private static boolean isTimeout(Throwable error) {
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause())
            if (cause instanceof java.util.concurrent.TimeoutException) return true;
        return false;
    }

    @Override
    public void cancel(String id) {
        control.cancel(id);
    }
}
