package com.iceeboot.framework.service.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;

import io.agentscope.core.agent.*;
import io.agentscope.core.event.*;
import io.agentscope.core.message.*;
import io.agentscope.core.middleware.*;
import io.agentscope.core.model.*;

import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.Function;

/** 每个 Agent 调用拥有独立账本；流内 usage 采用最后有效快照，每个模型请求只入账一次。 */
public final class RunRecorder implements MiddlewareBase {
    public final String id = UUID.randomUUID().toString();
    public final String rootId;
    private final String parentId;
    private final PublishedAgent snapshot;
    private final Caller caller;
    private final IAgentPersistenceService store;
    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean ended = new AtomicBoolean();
    private final AtomicInteger modelBudget;
    private final Runnable acquireQuota;
    private AgentSessionLease sessionLease;
    private final String traceId;
    private volatile String output = "";
    private static final int MAX_REASONING_CHARACTERS = 100_000;
    private final AtomicInteger modelRound = new AtomicInteger();
    private final Map<String,StringBuffer> reasoning = new java.util.concurrent.ConcurrentHashMap<>();
    private final StringBuffer partial = new StringBuffer();
    public String partialOutput() { return partial.toString(); }
    private java.util.function.Consumer<AgentProgress> progress = event -> {};
    private AtomicLong sequence = new AtomicLong();
    private final Map<String, StringBuilder> toolOutputs = new java.util.concurrent.ConcurrentHashMap<>();

    public void observeProgress(java.util.function.Consumer<AgentProgress> listener) { progress = listener; }

    public void progress(AgentProgressType type, String title, String text, String callId, String tool, String status) {
        synchronized (sequence) {
            progress.accept(new AgentProgress(sequence.incrementAndGet(), type, rootId, id, parentId,
                    snapshot.agent().code(), title, text, callId, tool, status, System.currentTimeMillis()));
        }
    }

    @Override
    public Flux<AgentEvent> onActing(Agent agent, RuntimeContext context, ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> {
            for (ToolUseBlock call : input.toolCalls()) {
                String title = call.getName().equals("load_skill_through_path") ? "加载技能" : call.getName().startsWith("agent_") ? "委派子智能体" : "调用工具";
                progress(AgentProgressType.TOOL_STARTED, title + " · " + call.getName(),
                        com.iceeboot.framework.service.agent.support.AgentProgressPreview.input(call.getInput()), call.getId(), call.getName(), "RUNNING");
            }
            return next.apply(input);
        });
    }

    private void publish(AgentEvent event) {
        if (event instanceof ModelCallStartEvent model) {
            if (!partial.isEmpty()) progress(AgentProgressType.PHASE, "阶段输出",
                    com.iceeboot.framework.service.agent.support.AgentProgressPreview.text(partialOutput()), "", "", "SUCCEEDED");
            partial.setLength(0);
            progress(AgentProgressType.MODEL_STARTED, "模型调用 · 第 " + modelRound.incrementAndGet() + " 轮", "", model.getReplyId(), "", "RUNNING");
        }
        else if (event instanceof ThinkingBlockStartEvent start) {
            reasoning.put(start.getReplyId() + ":" + start.getBlockId(),new StringBuffer());
            progress(AgentProgressType.PHASE,"正在接收模型分析 · 第 " + modelRound.get() + " 轮","内容由接入模型返回", "", "", "RUNNING");
        } else if (event instanceof ThinkingBlockDeltaEvent delta) {
            String key=delta.getReplyId() + ":" + delta.getBlockId();
            var content=reasoning.computeIfAbsent(key,ignored -> new StringBuffer());
            if (content.length()<MAX_REASONING_CHARACTERS) {
                String accepted=delta.getDelta().substring(0,Math.min(delta.getDelta().length(),MAX_REASONING_CHARACTERS-content.length()));
                content.append(accepted);
                progress(AgentProgressType.REASONING_DELTA,"模型分析 · 第 " + modelRound.get() + " 轮",accepted,key,"","RUNNING");
            }
        } else if (event instanceof ThinkingBlockEndEvent end) {
            completeReasoning(end.getReplyId() + ":" + end.getBlockId(),"SUCCEEDED");
        }
        else if (event instanceof TextBlockDeltaEvent text) {
            partial.append(text.getDelta());
            progress(AgentProgressType.TEXT_DELTA, "正在回复", text.getDelta(), text.getReplyId(), "", "RUNNING");
        }
        else if (event instanceof ToolResultTextDeltaEvent text) {
            var result = toolOutputs.computeIfAbsent(text.getToolCallId(), key -> new StringBuilder());
            synchronized (result) {
                if (result.length() < com.iceeboot.framework.service.agent.support.AgentProgressPreview.MAX_CHARACTERS * 2)
                    result.append(text.getDelta(), 0, Math.min(text.getDelta().length(), com.iceeboot.framework.service.agent.support.AgentProgressPreview.MAX_CHARACTERS * 2 - result.length()));
            }
        } else if (event instanceof ToolResultEndEvent result) {
            var output = toolOutputs.remove(result.getToolCallId());
            progress(AgentProgressType.TOOL_FINISHED, (result.getState().name().equals("DENIED") ? "工具被拒绝 · " : result.getState().name().equals("ERROR") ? "工具执行失败 · " : "工具返回 · ") + result.getToolCallName(),
                    com.iceeboot.framework.service.agent.support.AgentProgressPreview.text(output == null ? "" : output.toString()),
                    result.getToolCallId(), result.getToolCallName(), result.getState().name());
        }
    }

    private void completeReasoning(String key,String status) {
        var content=reasoning.remove(key);
        if(content==null || content.isEmpty()) return;
        String result=com.iceeboot.framework.service.agent.support.AgentProgressPreview.text(content.toString(),MAX_REASONING_CHARACTERS);
        if(content.length()>=MAX_REASONING_CHARACTERS) result+="\n…（模型分析达到展示上限，已截断）";
        progress(AgentProgressType.REASONING_FINISHED,"模型分析 · 第 " + modelRound.get() + " 轮",result,key,"",status);
    }

    public RunRecorder(
            String rootId,
            String parentId,
            PublishedAgent p,
            Caller caller,
            IAgentPersistenceService store,
            AtomicInteger budget) {
        this(rootId, parentId, p, caller, store, budget, () -> {});
    }

    public RunRecorder(
            String rootId,
            String parentId,
            PublishedAgent p,
            Caller caller,
            IAgentPersistenceService store,
            AtomicInteger budget,
            Runnable acquireQuota) {
        this(
                rootId,
                parentId,
                p,
                caller,
                store,
                budget,
                acquireQuota,
                com.iceeboot.framework.service.agent.support.AgentDiagnostics.traceId());
    }

    private RunRecorder(
            String rootId,
            String parentId,
            PublishedAgent p,
            Caller caller,
            IAgentPersistenceService store,
            AtomicInteger budget,
            Runnable acquireQuota,
            String traceId) {
        this.acquireQuota = acquireQuota;
        this.traceId = traceId;
        this.rootId = rootId == null ? id : rootId;
        this.parentId = parentId;
        this.snapshot = p;
        this.caller = caller;
        this.store = store;
        this.modelBudget = budget;
    }

    public void start(List<Msg> messages) {
        if (started.compareAndSet(false, true)) {
            store.startRun(id, rootId, parentId, snapshot, caller.userId(), store.json(messages));
            store.diagnose(id, "", traceId);
            progress(AgentProgressType.RUN_STARTED, parentId.isEmpty() ? "已收到请求，准备运行环境" : "子智能体开始处理 · " + snapshot.agent().name(), "", "", "", "RUNNING");
        }
    }

    public String traceId() {
        return traceId;
    }

    public void trackSession(AgentSessionLease lease) {
        sessionLease = lease;
    }

    public RunRecorder child(PublishedAgent child, Caller childCaller) {
        RunRecorder result = new RunRecorder(rootId, id, child, childCaller, store, modelBudget, acquireQuota, traceId);
        result.progress = progress;
        result.sequence = sequence;
        return result;
    }

    public void toolFailure(Throwable error) {
        com.iceeboot.framework.service.agent.support.AgentDiagnostics.failure(
                id, "TOOL", traceId, error);
    }

    public void fail(Throwable error) {
        if (isCancellation(error)) {
            finish("CANCELLED", ResponseMessage.Agent.EXECUTION_CANCELLED);
            return;
        }
        if (!ended.get()) {
            String code =
                    com.iceeboot.framework.service.agent.support.AgentDiagnostics.failure(
                            id, "EXECUTION", traceId, error);
            store.diagnose(id, code, traceId);
            finish("FAILED",error instanceof IceeBootException ? error.getMessage() : ResponseMessage.Agent.EXECUTION_FAILED);
        }
    }

    private static boolean isCancellation(Throwable error) {
        java.util.Set<Throwable> seen =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Throwable cause = error; cause != null && seen.add(cause); cause = cause.getCause())
            if (cause instanceof java.util.concurrent.CancellationException
                    || reactor.core.Exceptions.isCancel(cause)) return true;
        return false;
    }

    public void succeed(String result) {
        output = result;
        finish("SUCCEEDED", "");
    }

    public void fail() {
        finish("FAILED", ResponseMessage.Agent.EXECUTION_FAILED);
    }

    public void finish(String status, String error) {
        if (started.get() && ended.compareAndSet(false, true)) {
            for(String key:List.copyOf(reasoning.keySet())) completeReasoning(key,status);
            if (!status.equals("SUCCEEDED") && output.isEmpty()) output = partialOutput();
            store.finishRun(id, status, output, error);
            progress(AgentProgressType.AGENT_FINISHED, snapshot.agent().name() + (status.equals("SUCCEEDED") ? " · 处理完成" : " · " + error),
                    parentId.isEmpty() ? "" : com.iceeboot.framework.service.agent.support.AgentProgressPreview.text(output), "", "", status);
        }
    }

    public Model observe(Model delegate) {
        return new Model() {
            @Override
            public String getModelName() {
                return delegate.getModelName();
            }

            @Override
            public int getContextWindowSize() {
                return delegate.getContextWindowSize();
            }

            @Override
            public boolean supportsNativeStructuredOutput() {
                return delegate.supportsNativeStructuredOutput();
            }

            @Override
            public boolean supportsNativeStructuredOutputWithTools() {
                return delegate.supportsNativeStructuredOutputWithTools();
            }

            @Override
            public Flux<ChatResponse> stream(
                    List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                return Flux.defer(
                        () -> {
                            if (sessionLease != null) sessionLease.capture();
                            if (modelBudget.decrementAndGet() < 0)
                                return Flux.error(
                                        new IceeBootException(
                                                ResultCodeEnum.ERROR.getCode(),
                                                ResponseMessage.Agent
                                                        .EXECUTION_MODEL_BUDGET_EXCEEDED));
                            acquireQuota.run();
                            String callId = UUID.randomUUID().toString();
                            AtomicReference<ChatUsage> usage = new AtomicReference<>();
                            StringBuilder content = new StringBuilder();
                            AtomicBoolean saved = new AtomicBoolean();
                            Runnable save =
                                    () -> {
                                        if (saved.compareAndSet(false, true)) {
                                            ChatUsage u = usage.get();
                                            store.modelCall(
                                                    callId,
                                                    id,
                                                    rootId,
                                                    snapshot.agent().code(),
                                                    delegate.getModelName(),
                                                    store.json(messages),
                                                    content.toString(),
                                                    u == null ? 0 : u.getInputTokens(),
                                                    u == null ? 0 : u.getOutputTokens(),
                                                    u != null);
                                        }
                                    };
                            return delegate.stream(messages, tools, options)
                                    .doOnNext(
                                            r -> {
                                                if (r.getUsage() != null) usage.set(r.getUsage());
                                                if (r.getContent() != null)
                                                    content.append(store.json(r.getContent()))
                                                            .append('\n');
                                            })
                                    .doOnComplete(save)
                                    .doOnError(
                                            e -> {
                                                String code =
                                                        com.iceeboot.framework.service.agent.support
                                                                .AgentDiagnostics.failure(
                                                                id, "MODEL", traceId, e);
                                                store.diagnose(id, code, traceId);
                                                save.run();
                                            })
                                    .doOnCancel(save);
                        });
            }
        };
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext context,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        return Flux.defer(
                () -> {
                    start(input.msgs());
                    return next.apply(input)
                            .doOnNext(
                                    event -> {
                                        publish(event);
                                        if (event instanceof ExceedMaxItersEvent)
                                            throw new IceeBootException(
                                                    ResultCodeEnum.ERROR.getCode(),
                                                    ResponseMessage.Agent
                                                            .EXECUTION_MAX_ROUNDS_EXCEEDED);
                                        if (event instanceof AgentResultEvent result) {
                                            output = text(result.getResult());
                                            validate(output);
                                        }
                                    })
                            .doOnComplete(() -> finish("SUCCEEDED", ""))
                            .doOnError(this::fail)
                            .doOnCancel(
                                    () ->
                                            finish(
                                                    "CANCELLED",
                                                    ResponseMessage.Agent.EXECUTION_CANCELLED));
                });
    }

    private void validate(String value) {
        if (snapshot.agent().outputFormat() == OutputFormat.JSON_SCHEMA) {
            try {
                Object parsed = new ObjectMapper().readValue(value, Object.class);
                SchemaSupport.validate(snapshot.agent().outputSchema(), parsed);
            } catch (Exception e) {
                throw new IceeBootException(ResponseMessage.Agent.SCHEMA_OUTPUT_MISMATCH, e);
            }
        }
    }

    public static String text(Msg msg) {
        if (msg == null) return "";
        Object structured =
                msg.getMetadata() == null ? null : msg.getMetadata().get("_structured_output");
        if (structured != null) {
            try {
                return new ObjectMapper().writeValueAsString(structured);
            } catch (Exception e) {
                throw new IceeBootException(
                        ResponseMessage.Agent.SCHEMA_OUTPUT_SERIALIZATION_FAILED, e);
            }
        }
        return msg.getTextContent();
    }
}
