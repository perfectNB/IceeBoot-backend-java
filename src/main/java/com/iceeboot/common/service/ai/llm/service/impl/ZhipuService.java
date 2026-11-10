package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.LLMChoice;
import com.iceeboot.common.service.ai.llm.dto.LLMContentPart;
import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.dto.LLMUsage;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import com.iceeboot.common.service.ai.llm.security.PublicEndpointWebClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ZhipuService implements LLMService {
    private static final String DEFAULT_BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private static final String DEFAULT_MODEL = "glm-4.6-flash";
    private static final int DEFAULT_TIMEOUT_SECONDS = 120;

    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
            .build();
    private final WebClient publicEndpointWebClient = PublicEndpointWebClientFactory.create(16 * 1024 * 1024);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.ZHIPU;
    }

    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        long startedAt = System.currentTimeMillis();
        return webClient(request).post().uri(chatEndpoint(request.getBaseUrl()))
                .header("Authorization", "Bearer " + request.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody(request, false))
                .exchangeToMono(response -> response.bodyToMono(String.class).defaultIfEmpty("").map(body -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return error("智谱 API 调用失败（HTTP " + response.statusCode().value() + "）：" + errorMessage(body));
                    }
                    LLMResponse result = parse(body, false);
                    result.setResponseTime(System.currentTimeMillis() - startedAt);
                    return result;
                }))
                .timeout(timeout(request))
                .onErrorResume(exception -> {
                    log.error("智谱 API 调用异常: type={}", exception.getClass().getSimpleName());
                    return Mono.just(error("智谱 API 调用失败：" + exception.getMessage()));
                });
    }

    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        return webClient(request).post().uri(chatEndpoint(request.getBaseUrl()))
                .header("Authorization", "Bearer " + request.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody(request, true))
                .exchangeToFlux(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToFlux(String.class);
                    }
                    return response.bodyToMono(String.class).defaultIfEmpty("")
                            .flatMapMany(body -> Flux.<String>error(new IllegalStateException(
                                    "HTTP " + response.statusCode().value() + "：" + errorMessage(body))));
                })
                .timeout(timeout(request))
                .flatMapIterable(this::streamPayloads)
                .map(payload -> parse(payload, true))
                .onErrorResume(exception -> Flux.just(error("智谱流式 API 调用失败：" + exception.getMessage())));
    }

    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        return chat(new LLMRequest().setApiKey(apiKey).setBaseUrl(baseUrl)
                .setModel(DEFAULT_MODEL).setUserMessage("你好").setMaxTokens(1))
                .map(response -> !StringUtils.hasText(response.getError())).onErrorReturn(false);
    }

    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        return Mono.just(List.of("glm-4.6-flash", "glm-4.6v-flash", "glm-4.6v-flashx", "glm-4.6v"));
    }

    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    public Integer getMaxTokens(String model) {
        return 8192;
    }

    private Map<String, Object> requestBody(LLMRequest request, boolean stream) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", StringUtils.hasText(request.getModel()) ? request.getModel() : DEFAULT_MODEL);
        body.put("stream", stream);
        List<Map<String, Object>> messages = new ArrayList<>();
        boolean hasUserMessage = StringUtils.hasText(request.getUserMessage()) || request.getMessages() != null
                && request.getMessages().stream().anyMatch(item -> "user".equalsIgnoreCase(item.getRole())
                && (StringUtils.hasText(item.getContent()) || item.getContentParts() != null && !item.getContentParts().isEmpty()));
        if (StringUtils.hasText(request.getSystemPrompt())) {
            messages.add(message(hasUserMessage ? "system" : "user", request.getSystemPrompt()));
        }
        if (request.getMessages() != null) {
            request.getMessages().stream().filter(item -> StringUtils.hasText(item.getRole())
                            && (StringUtils.hasText(item.getContent()) || item.getContentParts() != null && !item.getContentParts().isEmpty()))
                    .forEach(item -> messages.add(message(item.getRole(), messageContent(item))));
        }
        if (StringUtils.hasText(request.getUserMessage())) messages.add(message("user", request.getUserMessage()));
        body.put("messages", messages);
        if (request.getTemperature() != null) body.put("temperature", request.getTemperature());
        if (request.getMaxTokens() != null) body.put("max_tokens", request.getMaxTokens());
        appendThinkingOptions(body, request);
        if (request.getExtraParams() != null) body.putAll(request.getExtraParams());
        return body;
    }

    /** 处理模型原生思考参数及固定开启思考的约束。 */
    private void appendThinkingOptions(Map<String, Object> body, LLMRequest request) {
        boolean alwaysThinking = isAlwaysThinkingModel(request.getModel());
        if (alwaysThinking) {
            // 未显式指定模式时沿用厂商默认值。
            if (request.getThinking() == null) {
                if (StringUtils.hasText(request.getReasoningEffort())) {
                    body.put("reasoning_effort", glm53ReasoningEffort(request.getReasoningEffort()));
                }
                return;
            }
            if (Boolean.FALSE.equals(request.getThinking())) {
                throw new IllegalArgumentException("所选 GLM-5.3 模型不支持关闭思考");
            }
            body.put("thinking", Map.of("type", "enabled"));
            body.put("reasoning_effort", glm53ReasoningEffort(request.getReasoningEffort()));
            return;
        }
        if (request.getThinking() != null) {
            body.put("thinking", Map.of("type", request.getThinking() ? "enabled" : "disabled"));
        }
        if (supportsFlexibleReasoningEffort(request.getModel())
                && StringUtils.hasText(request.getReasoningEffort())
                && !"none".equalsIgnoreCase(request.getReasoningEffort())) {
            body.put("reasoning_effort", flexibleReasoningEffort(request.getReasoningEffort()));
        }
    }

    private boolean isAlwaysThinkingModel(String model) {
        return StringUtils.hasText(model) && model.trim().toLowerCase().startsWith("glm-5.3");
    }

    private boolean supportsFlexibleReasoningEffort(String model) {
        return StringUtils.hasText(model) && model.trim().toLowerCase().startsWith("glm-5.2");
    }

    private String glm53ReasoningEffort(String value) {
        if (!StringUtils.hasText(value)) return "high";
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "low", "minimal" -> "low";
            case "medium", "high" -> "high";
            case "xhigh", "max", "ultra" -> "max";
            default -> "high";
        };
    }

    private String flexibleReasoningEffort(String value) {
        String normalized = value.trim().toLowerCase();
        return switch (normalized) {
            case "minimal", "low", "medium", "high", "xhigh", "max" -> normalized;
            default -> "high";
        };
    }

    private Map<String, Object> message(String role, Object content) {
        return Map.of("role", role, "content", content);
    }

    private Object messageContent(LLMMessage message) {
        if (message.getContentParts() == null || message.getContentParts().isEmpty()) return message.getContent();
        return message.getContentParts().stream().map(this::contentPart).toList();
    }

    private Map<String, Object> contentPart(LLMContentPart part) {
        if (LLMContentPart.TYPE_TEXT.equals(part.getType())) {
            return Map.of("type", "text", "text", part.getText());
        }
        if (LLMContentPart.TYPE_IMAGE_URL.equals(part.getType()) && part.getImageUrl() != null) {
            return Map.of("type", "image_url", "image_url",
                    Map.of("url", part.getImageUrl().getUrl()));
        }
        throw new IllegalArgumentException("不支持的智谱消息内容类型：" + part.getType());
    }

    private LLMResponse parse(String body, boolean streaming) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("error")) return error(errorMessage(body));
            LLMResponse response = new LLMResponse().setProvider(getProvider()).setFinished(!streaming)
                    .setId(text(root, "id")).setModel(text(root, "model"));
            if (root.has("created")) response.setCreated(root.get("created").asLong());
            List<LLMChoice> choices = new ArrayList<>();
            for (JsonNode source : root.path("choices")) {
                String finishReason = text(source, "finish_reason");
                JsonNode contentNode = streaming ? source.path("delta") : source.path("message");
                String content = text(contentNode, "content");
                LLMMessage value = new LLMMessage().setRole(text(contentNode, "role")).setContent(content);
                LLMChoice choice = new LLMChoice().setIndex(source.path("index").asInt(0)).setFinishReason(finishReason);
                if (streaming) choice.setDelta(value); else choice.setMessage(value);
                choices.add(choice);
                String reasoning = text(contentNode, "reasoning_content");
                if (reasoning == null) reasoning = text(contentNode, "reasoning");
                if (StringUtils.hasText(reasoning)) {
                    if (response.getExtraData() == null) response.setExtraData(new LinkedHashMap<>());
                    response.getExtraData().put("reasoning_content_delta", reasoning);
                    response.getExtraData().put("reasoning_type", "content");
                }
                if (StringUtils.hasText(finishReason)) response.setFinished(true);
            }
            response.setChoices(choices);
            if (root.has("usage")) {
                JsonNode usage = root.get("usage");
                response.setUsage(new LLMUsage().setPromptTokens(usage.path("prompt_tokens").asInt())
                        .setCompletionTokens(usage.path("completion_tokens").asInt())
                        .setTotalTokens(usage.path("total_tokens").asInt()));
            }
            return response;
        } catch (Exception exception) {
            return error("智谱响应解析失败：" + exception.getMessage());
        }
    }

    private String chatEndpoint(String configuredBaseUrl) {
        String base = StringUtils.hasText(configuredBaseUrl) ? configuredBaseUrl.trim() : DEFAULT_BASE_URL;
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        if ("https://open.bigmodel.cn".equalsIgnoreCase(base)) base += "/api/paas/v4";
        return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
    }

    private WebClient webClient(LLMRequest request) {
        return Boolean.TRUE.equals(request.getPublicEndpointOnly()) ? publicEndpointWebClient : webClient;
    }

    private Duration timeout(LLMRequest request) {
        return Duration.ofSeconds(request.getTimeout() == null ? DEFAULT_TIMEOUT_SECONDS : request.getTimeout());
    }

    private List<String> streamPayloads(String chunk) {
        if (!StringUtils.hasText(chunk)) return List.of();
        List<String> values = new ArrayList<>();
        for (String raw : chunk.replace("\r", "").split("\n")) {
            String line = raw.trim();
            if (line.startsWith("data:")) line = line.substring(5).trim();
            if (!line.isEmpty() && !line.startsWith(":") && !"[DONE]".equalsIgnoreCase(line)) values.add(line);
        }
        return values;
    }

    private String errorMessage(String body) {
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            String message = text(error, "message");
            return StringUtils.hasText(message) ? message : body;
        } catch (Exception ignored) {
            return body;
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private LLMResponse error(String message) {
        String safe = StringUtils.hasText(message) ? message.replaceAll("\\s+", " ").trim() : "上游未返回错误详情";
        if (safe.length() > 1000) safe = safe.substring(0, 1000) + "...";
        return new LLMResponse().setProvider(getProvider()).setError(safe).setFinished(true);
    }
}
