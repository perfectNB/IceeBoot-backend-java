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
    private volatile String output = "";

    public RunRecorder(
            String rootId,
            String parentId,
            PublishedAgent p,
            Caller caller,
            IAgentPersistenceService store,
            AtomicInteger budget) {
        this.rootId = rootId == null ? id : rootId;
        this.parentId = parentId;
        this.snapshot = p;
        this.caller = caller;
        this.store = store;
        this.modelBudget = budget;
    }

    public void start(List<Msg> messages) {
        if (started.compareAndSet(false, true))
            store.startRun(id, rootId, parentId, snapshot, caller.userId(), store.json(messages));
    }

    public void fail() {
        finish("FAILED", ResponseMessage.Agent.EXECUTION_FAILED);
    }

    public void finish(String status, String error) {
        if (started.get() && ended.compareAndSet(false, true))
            store.finishRun(id, status, output, error);
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
                            if (modelBudget.decrementAndGet() < 0)
                                return Flux.error(
                                        new IceeBootException(
                                                ResultCodeEnum.ERROR.getCode(),
                                                ResponseMessage.Agent
                                                        .EXECUTION_MODEL_BUDGET_EXCEEDED));
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
                                    .doOnError(e -> save.run())
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
                            .doOnError(e -> fail())
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
