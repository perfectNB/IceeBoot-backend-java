package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.enums.ResultCodeEnum;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.security.AgentSecrets;

import io.agentscope.core.model.*;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** 复用 SDK 模型协议实现；不再逐供应商维护 HTTP、SSE 或工具调用循环。 */
@Component
public class AgentModelFactory {
    private final AgentSecrets secrets;

    public AgentModelFactory(AgentSecrets secrets) {
        this.secrets = secrets;
    }

    public Model create(ModelConfig m, Generation overrides) {
        try {
            return createClient(m, overrides);
        } catch (IceeBootException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IceeBootException(
                    ResultCodeEnum.ERROR.getCode(),
                    ResponseMessage.Agent.MODEL_CREATE_CLIENT_FAILED,
                    e);
        }
    }

    private Model createClient(ModelConfig m, Generation overrides) {
        if (!m.enabled()) throw new IceeBootException(ResponseMessage.Agent.MODEL_DISABLED);
        Generation g = merge(m.generation(), overrides);
        ModelCreationContext context =
                ModelCreationContext.builder()
                        .apiKey(secrets.decrypt(m.apiKey()))
                        .baseUrl(m.baseUrl())
                        .stream(true)
                        .enableThinking(g.thinking())
                        .cachePolicy(CachePolicy.DISABLED)
                        .option(
                                "contextWindowSize",
                                m.contextWindow() == null ? 32768 : m.contextWindow())
                        .component(GenerateOptions.class, options(m, overrides))
                        .build();
        return ModelRegistry.resolve(prefix(m) + ":" + m.model(), context);
    }

    public GenerateOptions options(ModelConfig m, Generation overrides) {
        Generation g = merge(m.generation(), overrides);
        Map<String, Object> extra = new LinkedHashMap<>(g.extra());
        if (g.thinking() != null) {
            switch (m.provider()) {
                case QWEN -> extra.put("enable_thinking", g.thinking());
                case DEEPSEEK, GLM, KIMI ->
                        extra.put(
                                "thinking", Map.of("type", g.thinking() ? "enabled" : "disabled"));
                default -> {
                    /* GPT、Claude、Gemini 由对应客户端处理通用思考参数。 */
                }
            }
        }
        return GenerateOptions.builder().modelName(m.model()).stream(true)
                .temperature(g.temperature())
                .maxTokens(g.maxTokens())
                .reasoningEffort(g.reasoningEffort())
                .thinkingBudget(g.thinkingBudget())
                .additionalBodyParams(extra)
                .executionConfig(
                        ExecutionConfig.builder()
                                .maxAttempts(1)
                                .timeout(Duration.ofSeconds(120))
                                .build())
                .build();
    }

    private String prefix(ModelConfig m) {
        return switch (m.protocol()) {
            case ANTHROPIC -> "anthropic";
            case GEMINI -> "gemini";
            case OPENAI_RESPONSES -> "openai-official";
            case OPENAI_CHAT ->
                    switch (m.provider()) {
                        case DEEPSEEK -> "deepseek";
                        case GLM -> "glm";
                        case KIMI -> "kimi";
                        case MINIMAX -> "minimax";
                        default -> "openai";
                    };
        };
    }

    private Generation merge(Generation a, Generation b) {
        if (a == null) a = new Generation(null, null, null, null, null, Map.of());
        if (b == null) b = new Generation(null, null, null, null, null, Map.of());
        Map<String, Object> extra = new LinkedHashMap<>();
        if (a.extra() != null) extra.putAll(a.extra());
        if (b.extra() != null) extra.putAll(b.extra());
        return new Generation(
                b.temperature() != null ? b.temperature() : a.temperature(),
                b.maxTokens() != null ? b.maxTokens() : a.maxTokens(),
                b.thinking() != null ? b.thinking() : a.thinking(),
                b.reasoningEffort() != null ? b.reasoningEffort() : a.reasoningEffort(),
                b.thinkingBudget() != null ? b.thinkingBudget() : a.thinkingBudget(),
                extra);
    }
}
