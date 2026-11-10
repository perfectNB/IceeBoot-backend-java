package com.iceeboot.common.service.ai.llm.utils;

import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 大模型提示词日志工具。
 *
 * <p>统一格式化不同调用栈下发送给 LLM 的请求内容，
 * 便于排查提示词拼接、模型路由和工具调用上下文问题。</p>
 */
public final class LlmPromptLogUtils {

    private LlmPromptLogUtils() {
    }

    public static String buildSpringAiPromptLog(String action, String executorClassName, Prompt prompt) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("LLM_PROMPT source=SPRING_AI")
                .append(" action=").append(safe(action))
                .append(" executor=").append(safe(executorClassName))
                .append('\n');

        if (prompt == null) {
            sb.append("prompt=<null>");
            return sb.toString();
        }

        appendSpringAiOptions(sb, prompt.getOptions());
        appendSpringAiMessages(sb, prompt.getInstructions());
        return sb.toString();
    }

    public static String buildLlmRequestLog(String action, String provider, LLMRequest request) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("LLM_PROMPT source=CUSTOM_LLM")
                .append(" action=").append(safe(action))
                .append(" provider=").append(safe(provider))
                .append('\n');

        if (request == null) {
            sb.append("request=<null>");
            return sb.toString();
        }

        sb.append("model=").append(safe(request.getModel()))
                .append('\n')
                .append("temperature=").append(valueOrNull(request.getTemperature()))
                .append('\n')
                .append("maxTokens=").append(valueOrNull(request.getMaxTokens()))
                .append('\n')
                .append("timeoutSeconds=").append(valueOrNull(request.getTimeout()))
                .append('\n')
                .append("stream=").append(valueOrNull(request.getStream()))
                .append('\n');

        if (request.getExtraParams() != null && !request.getExtraParams().isEmpty()) {
            sb.append("extraParamKeys=").append(request.getExtraParams().keySet())
                    .append('\n');
        }

        if (StringUtils.hasText(request.getSystemPrompt())) {
            appendBlock(sb, "systemPrompt", request.getSystemPrompt());
        }
        if (StringUtils.hasText(request.getUserMessage())) {
            appendBlock(sb, "userMessage", request.getUserMessage());
        }
        if (!CollectionUtils.isEmpty(request.getMessages())) {
            sb.append("messages:\n");
            for (int i = 0; i < request.getMessages().size(); i++) {
                LLMMessage message = request.getMessages().get(i);
                if (message == null) {
                    continue;
                }
                sb.append("[").append(i).append("] role=").append(safe(message.getRole()));
                if (StringUtils.hasText(message.getName())) {
                    sb.append(" name=").append(message.getName().trim());
                }
                sb.append('\n');
                sb.append(safe(message.getContent())).append('\n');
            }
        }
        return sb.toString();
    }

    private static void appendSpringAiOptions(StringBuilder sb, ChatOptions options) {
        if (options == null) {
            sb.append("options=<null>\n");
            return;
        }
        sb.append("model=").append(safe(options.getModel())).append('\n')
                .append("temperature=").append(valueOrNull(options.getTemperature())).append('\n')
                .append("topP=").append(valueOrNull(options.getTopP())).append('\n')
                .append("topK=").append(valueOrNull(options.getTopK())).append('\n')
                .append("maxTokens=").append(valueOrNull(options.getMaxTokens())).append('\n')
                .append("frequencyPenalty=").append(valueOrNull(options.getFrequencyPenalty())).append('\n')
                .append("presencePenalty=").append(valueOrNull(options.getPresencePenalty())).append('\n');

        if (!CollectionUtils.isEmpty(options.getStopSequences())) {
            sb.append("stopSequences=").append(options.getStopSequences()).append('\n');
        }

        if (options instanceof ToolCallingChatOptions toolOptions) {
            List<String> toolNames = resolveToolNames(toolOptions);
            if (!toolNames.isEmpty()) {
                sb.append("tools=").append(toolNames).append('\n');
            }
            if (toolOptions.getToolContext() != null && !toolOptions.getToolContext().isEmpty()) {
                sb.append("toolContextKeys=").append(toolOptions.getToolContext().keySet()).append('\n');
            }
        }
    }

    private static void appendSpringAiMessages(StringBuilder sb, List<Message> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            sb.append("messages=<empty>");
            return;
        }
        sb.append("messages:\n");
        for (int i = 0; i < messages.size(); i++) {
            Message message = messages.get(i);
            if (message == null) {
                continue;
            }
            sb.append("[").append(i).append("] type=").append(message.getMessageType()).append('\n');
            sb.append(safe(message.getText())).append('\n');
        }
    }

    private static List<String> resolveToolNames(ToolCallingChatOptions toolOptions) {
        List<String> toolNames = new ArrayList<>();
        Set<String> declaredToolNames = toolOptions.getToolNames();
        if (!CollectionUtils.isEmpty(declaredToolNames)) {
            toolNames.addAll(declaredToolNames);
        }
        List<ToolCallback> callbacks = toolOptions.getToolCallbacks();
        if (!CollectionUtils.isEmpty(callbacks)) {
            toolNames.addAll(callbacks.stream()
                    .map(LlmPromptLogUtils::resolveToolName)
                    .filter(StringUtils::hasText)
                    .toList());
        }
        return toolNames.stream().distinct().collect(Collectors.toList());
    }

    private static String resolveToolName(ToolCallback callback) {
        if (callback == null) {
            return "";
        }
        try {
            ToolDefinition toolDefinition = callback.getToolDefinition();
            if (toolDefinition != null && StringUtils.hasText(toolDefinition.name())) {
                return toolDefinition.name().trim();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return callback.getClass().getSimpleName();
    }

    private static void appendBlock(StringBuilder sb, String title, String content) {
        sb.append(title).append(":\n");
        sb.append(content == null ? "" : content).append('\n');
    }

    private static String safe(String value) {
        return StringUtils.hasText(value) ? value.trim() : "-";
    }

    private static String valueOrNull(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
