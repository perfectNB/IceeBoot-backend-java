package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.*;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import com.iceeboot.common.service.ai.llm.security.PublicEndpointWebClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ChatGPT大模型服务实现类
 * 使用 OpenAI Responses API 格式（/v1/responses）
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class ChatGPTService implements LLMService {

    private static final String DEFAULT_BASE_URL = "https://gmncodex.com";
    private static final String DEFAULT_MODEL = "gpt-5.6-sol";
    private static final Integer DEFAULT_TIMEOUT = 120;

    private final WebClient webClient;
    private final WebClient publicEndpointWebClient;
    private final ObjectMapper objectMapper;

    public ChatGPTService() {
        this.webClient = WebClient.builder()
                .defaultHeader("User-Agent", "curl/8.7.1")
                .defaultHeader("Accept", "*/*")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.publicEndpointWebClient = PublicEndpointWebClientFactory.create(10 * 1024 * 1024);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.CHATGPT;
    }

    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        ApiPlan plan = resolveApiPlan(request);
        Mono<LLMResponse> result = executeChat(request, plan.primary());
        if (plan.fallback() != null) {
            result = result.onErrorResume(error -> shouldFallback(error), error -> {
                log.warn("自有 GPT Responses 接口不兼容，回退 Chat Completions: type={}", error.getClass().getSimpleName());
                return executeChat(request, plan.fallback());
            });
        }
        return result
                .doOnError(error -> log.error("ChatGPT API调用失败: type={}", error.getClass().getSimpleName()))
                .onErrorResume(error -> Mono.just(createErrorResponse(
                        "API调用失败：" + safeErrorMessage(error.getMessage()))));
    }

    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        ApiPlan plan = resolveApiPlan(request);
        AtomicBoolean emitted = new AtomicBoolean(false);
        Flux<LLMResponse> result = executeChatStream(request, plan.primary())
                .doOnNext(response -> emitted.set(true));
        if (plan.fallback() != null) {
            result = result.onErrorResume(error -> !emitted.get() && shouldFallback(error), error -> {
                log.warn("自有 GPT 流式 Responses 接口不兼容，回退 Chat Completions: type={}", error.getClass().getSimpleName());
                return executeChatStream(request, plan.fallback());
            });
        }
        return result
                .doOnError(error -> log.error("ChatGPT流式API调用失败: type={}", error.getClass().getSimpleName()))
                .onErrorResume(error -> Flux.just(createErrorResponse(
                        "流式API调用失败：" + safeErrorMessage(error.getMessage()))));
    }

    private Mono<LLMResponse> executeChat(LLMRequest request, ApiTarget target) {
        return buildRequestBody(request, false, target.protocol())
                .flatMap(requestBody -> {
                    log.debug("ChatGPT请求: protocol={}, customEndpoint={}", target.protocol(),
                            Boolean.TRUE.equals(request.getPublicEndpointOnly()));
                    return requestSpec(request, target, "application/json")
                            .bodyValue(requestBody)
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(String.class);
                                }
                                int status = response.statusCode().value();
                                return response.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMap(body -> Mono.error(upstreamError(status, body, request.getApiKey())));
                            });
                })
                .timeout(requestTimeout(request))
                .map(body -> target.protocol() == ApiProtocol.RESPONSES
                        ? parseResponse(body) : parseChatCompletionsResponse(body));
    }

    private Flux<LLMResponse> executeChatStream(LLMRequest request, ApiTarget target) {
        return buildRequestBody(request, true, target.protocol())
                .flatMapMany(requestBody -> requestSpec(request, target, "text/event-stream")
                        .bodyValue(requestBody)
                        .exchangeToFlux(response -> {
                            if (response.statusCode().is2xxSuccessful()) {
                                return response.bodyToFlux(String.class);
                            }
                            int status = response.statusCode().value();
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMapMany(body -> Flux.error(upstreamError(status, body, request.getApiKey())));
                        }))
                .timeout(requestTimeout(request))
                .concatMapIterable(this::extractStreamPayloads)
                .map(payload -> target.protocol() == ApiProtocol.RESPONSES
                        ? parseStreamResponse(payload) : parseChatCompletionsStreamResponse(payload))
                .filter(Objects::nonNull);
    }

    private WebClient.RequestBodySpec requestSpec(LLMRequest request, ApiTarget target, String accept) {
        return webClient(request).post()
                .uri(target.url())
                .header("User-Agent", "curl/8.7.1")
                .header("Authorization", "Bearer " + request.getApiKey())
                .header("Content-Type", "application/json")
                .header("Accept", accept);
    }

    private Duration requestTimeout(LLMRequest request) {
        return Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT);
    }

    private WebClient webClient(LLMRequest request) {
        return Boolean.TRUE.equals(request.getPublicEndpointOnly()) ? publicEndpointWebClient : webClient;
    }

    private String safeErrorMessage(String message) {
        String normalized = message == null || message.isBlank() ? "未知错误" : message.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
    }

    private UpstreamApiException upstreamError(int status, String body, String apiKey) {
        String safeBody = safeErrorBody(body, apiKey);
        log.error("ChatGPT API返回错误: status={}", status);
        String suffix = safeBody.isBlank() ? "" : "，上游信息：" + safeBody;
        return new UpstreamApiException(status, "HTTP " + status + suffix);
    }

    private String safeErrorBody(String body, String apiKey) {
        String safe = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (apiKey != null && !apiKey.isBlank()) safe = safe.replace(apiKey, "***");
        return safe.length() > 500 ? safe.substring(0, 500) + "..." : safe;
    }

    private boolean shouldFallback(Throwable error) {
        if (!(error instanceof UpstreamApiException upstream)) return false;
        return upstream.status() == 400
                || upstream.status() == 404
                || upstream.status() == 405
                || upstream.status() == 415
                || upstream.status() == 422
                || upstream.status() == 501;
    }

    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        LLMRequest testRequest = new LLMRequest()
                .setApiKey(apiKey)
                .setBaseUrl(baseUrl)
                .setUserMessage("Hello")
                .setMaxTokens(1);

        return chat(testRequest)
                .map(response -> response.getError() == null)
                .onErrorReturn(false);
    }

    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "gpt-5.6-sol",
                "gpt-5.6-terra",
                "gpt-5.6-luna",
                "gpt-5.4",
                "gpt-4o",
                "gpt-4o-mini"
        );
        return Mono.just(models);
    }

    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }

    @Override
    public Integer getMaxTokens(String model) {
        return 16384;
    }

    private ApiPlan resolveApiPlan(LLMRequest request) {
        String baseUrl = normalizedBaseUrl(request.getBaseUrl());
        if (endsWithPath(baseUrl, "/responses")) {
            ApiTarget fallback = Boolean.TRUE.equals(request.getPublicEndpointOnly())
                    ? new ApiTarget(ApiProtocol.CHAT_COMPLETIONS,
                    baseUrl.substring(0, baseUrl.length() - "/responses".length()) + "/chat/completions")
                    : null;
            return new ApiPlan(new ApiTarget(ApiProtocol.RESPONSES, baseUrl), fallback);
        }
        if (endsWithPath(baseUrl, "/chat/completions")) {
            return new ApiPlan(new ApiTarget(ApiProtocol.CHAT_COMPLETIONS, baseUrl), null);
        }

        ApiTarget primary = new ApiTarget(ApiProtocol.RESPONSES, buildResponsesEndpoint(baseUrl));
        ApiTarget fallback = Boolean.TRUE.equals(request.getPublicEndpointOnly())
                ? new ApiTarget(ApiProtocol.CHAT_COMPLETIONS, buildChatCompletionsEndpoint(baseUrl))
                : null;
        return new ApiPlan(primary, fallback);
    }

    private String buildResponsesEndpoint(String configuredBaseUrl) {
        String baseUrl = normalizedBaseUrl(configuredBaseUrl);
        if (endsWithPath(baseUrl, "/responses")) return baseUrl;
        return appendEndpoint(baseUrl, "responses");
    }

    private String buildChatCompletionsEndpoint(String configuredBaseUrl) {
        String baseUrl = normalizedBaseUrl(configuredBaseUrl);
        if (endsWithPath(baseUrl, "/chat/completions")) return baseUrl;
        return appendEndpoint(baseUrl, "chat/completions");
    }

    private String normalizedBaseUrl(String configuredBaseUrl) {
        String baseUrl = configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? DEFAULT_BASE_URL : configuredBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String appendEndpoint(String baseUrl, String endpoint) {
        return baseUrl.endsWith("/v1") ? baseUrl + "/" + endpoint : baseUrl + "/v1/" + endpoint;
    }

    private boolean endsWithPath(String baseUrl, String path) {
        return baseUrl.toLowerCase(Locale.ROOT).endsWith(path);
    }

    /**
     * 构建 Responses API 请求体
     */
    private Mono<Map<String, Object>> buildRequestBody(LLMRequest request, boolean stream) {
        return buildRequestBody(request, stream, ApiProtocol.RESPONSES);
    }

    private Mono<Map<String, Object>> buildRequestBody(LLMRequest request, boolean stream, ApiProtocol protocol) {
        return protocol == ApiProtocol.RESPONSES
                ? buildResponsesRequestBody(request, stream)
                : buildChatCompletionsRequestBody(request, stream);
    }

    private Mono<Map<String, Object>> buildResponsesRequestBody(LLMRequest request, boolean stream) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new LinkedHashMap<>();

            String model = request.getModel();
            if (model == null || model.trim().isEmpty()) {
                model = DEFAULT_MODEL;
            }
            requestBody.put("model", model);
            requestBody.put("stream", stream);

            // 系统提示词使用 instructions 参数
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
                requestBody.put("instructions", request.getSystemPrompt());
            }

            // 构建 input 消息列表（Responses API 格式）
            List<Map<String, Object>> input = new ArrayList<>();

            // 添加历史消息
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                for (LLMMessage msg : request.getMessages()) {
                    input.add(buildInputMessage(msg));
                }
            } else if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
                input.add(buildInputMessage("user", request.getUserMessage()));
            }

            requestBody.put("input", input);

            if (request.getMaxTokens() != null) {
                requestBody.put("max_output_tokens", request.getMaxTokens());
            }

            if (request.getTemperature() != null) requestBody.put("temperature", request.getTemperature());

            // 添加扩展参数
            if (request.getExtraParams() != null) {
                requestBody.putAll(request.getExtraParams());
            }
            String reasoningEffort = resolveReasoningEffort(request, model);
            if (reasoningEffort != null) {
                Map<String, Object> reasoning = new LinkedHashMap<>();
                reasoning.put("effort", reasoningEffort);
                // OpenAI 不开放原始隐藏思维链；思考做题时主动请求接口可提供的最详细摘要。
                if (Boolean.TRUE.equals(request.getThinking()) && !"none".equals(reasoningEffort)) {
                    reasoning.put("summary", "auto");
                }
                requestBody.put("reasoning", reasoning);
                if (!"none".equals(reasoningEffort)) {
                    requestBody.remove("temperature"); requestBody.remove("top_p"); requestBody.remove("top_logprobs");
                }
            }

            return requestBody;
        });
    }

    /**
     * 构建兼容 OpenAI Chat Completions 的请求体。仅在用户自有 GPT 的通用地址
     * 无法处理 Responses API 时使用，平台内置模型仍保持原来的 Responses 协议。
     */
    private Mono<Map<String, Object>> buildChatCompletionsRequestBody(LLMRequest request, boolean stream) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            String model = request.getModel();
            if (model == null || model.trim().isEmpty()) model = DEFAULT_MODEL;
            requestBody.put("model", model);
            requestBody.put("stream", stream);

            List<Map<String, Object>> messages = new ArrayList<>();
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
                messages.add(buildChatMessage(LLMMessage.system(request.getSystemPrompt())));
            }
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                for (LLMMessage message : request.getMessages()) {
                    messages.add(buildChatMessage(message));
                }
            } else if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
                messages.add(buildChatMessage(LLMMessage.user(request.getUserMessage())));
            }
            requestBody.put("messages", messages);

            if (request.getMaxTokens() != null) requestBody.put("max_tokens", request.getMaxTokens());
            if (request.getTemperature() != null) requestBody.put("temperature", request.getTemperature());
            if (request.getExtraParams() != null) requestBody.putAll(request.getExtraParams());
            String reasoningEffort = resolveReasoningEffort(request, model);
            if (reasoningEffort != null) {
                requestBody.put("reasoning_effort", reasoningEffort);
                requestBody.remove("max_tokens");
                if (request.getMaxTokens() != null) requestBody.put("max_completion_tokens", request.getMaxTokens());
                if (!"none".equals(reasoningEffort)) {
                    requestBody.remove("temperature"); requestBody.remove("top_p"); requestBody.remove("top_logprobs"); requestBody.remove("logprobs");
                }
            }
            return requestBody;
        });
    }

    private Map<String, Object> buildChatMessage(LLMMessage source) {
        Map<String, Object> message = new LinkedHashMap<>();
        String role = source == null || source.getRole() == null || source.getRole().isBlank()
                ? "user" : source.getRole();
        message.put("role", role);
        if (source != null && source.getContentParts() != null && !source.getContentParts().isEmpty()) {
            List<Map<String, Object>> content = new ArrayList<>();
            for (LLMContentPart part : source.getContentParts()) {
                content.add(buildChatContentPart(part));
            }
            message.put("content", content);
        } else {
            message.put("content", source == null || source.getContent() == null ? "" : source.getContent());
        }
        if (source != null && source.getName() != null && !source.getName().isBlank()) {
            message.put("name", source.getName());
        }
        return message;
    }

    private Map<String, Object> buildChatContentPart(LLMContentPart part) {
        if (part == null || part.getType() == null) {
            throw new IllegalArgumentException("ChatGPT 消息内容不能为空");
        }
        if (LLMContentPart.TYPE_TEXT.equals(part.getType())) {
            return Map.of("type", "text", "text", part.getText() == null ? "" : part.getText());
        }
        if (LLMContentPart.TYPE_IMAGE_URL.equals(part.getType()) && part.getImageUrl() != null) {
            Map<String, Object> imageUrl = new LinkedHashMap<>();
            imageUrl.put("url", part.getImageUrl().getUrl());
            if (part.getImageUrl().getDetail() != null && !part.getImageUrl().getDetail().isBlank()) {
                imageUrl.put("detail", "original".equals(part.getImageUrl().getDetail()) ? "high" : part.getImageUrl().getDetail());
            }
            return Map.of("type", "image_url", "image_url", imageUrl);
        }
        throw new IllegalArgumentException("不支持的 ChatGPT 消息内容类型：" + part.getType());
    }

    private String resolveReasoningEffort(LLMRequest request, String model) {
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        boolean reasoningModel = supportsNativeReasoning(normalizedModel);
        String configured = request.getReasoningEffort() == null || request.getReasoningEffort().isBlank()
                ? null : request.getReasoningEffort().trim().toLowerCase(Locale.ROOT);
        if (!reasoningModel) return null;
        if (Boolean.FALSE.equals(request.getThinking()) || "none".equals(configured)) {
            if (!supportsDisabledReasoning(normalizedModel)) {
                throw new IllegalArgumentException("所选 GPT 模型不支持关闭思考，请选择支持 none 的模型或启用思考");
            }
            return "none";
        }
        if (Boolean.TRUE.equals(request.getThinking()) && configured == null) return "high";
        return configured == null ? null : openAiReasoningEffort(configured, normalizedModel);
    }

    public static boolean supportsNativeReasoning(String model) {
        String value = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        return !value.contains("chat-latest") && (value.startsWith("o1") || value.startsWith("o3")
                || value.startsWith("o4") || value.startsWith("gpt-5") || value.startsWith("gpt-6"));
    }

    public static boolean supportsDisabledReasoning(String model) {
        model = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (model.contains("pro")) return false;
        if (model.startsWith("gpt-6-sol") || model.startsWith("gpt-6-luna") || model.startsWith("gpt-6.1-terra")) return true;
        if (model.startsWith("gpt-6")) return false;
        if (!model.startsWith("gpt-5.")) return false;
        int versionStart = "gpt-5.".length();
        int versionEnd = versionStart;
        while (versionEnd < model.length() && Character.isDigit(model.charAt(versionEnd))) versionEnd++;
        if (versionEnd == versionStart) return false;
        try {
            return Integer.parseInt(model.substring(versionStart, versionEnd)) >= 1;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String openAiReasoningEffort(String value, String model) {
        if (model.startsWith("gpt-5") && model.contains("pro")) return "high";
        return switch (value) {
            case "none" -> "none";
            case "minimal" -> "minimal";
            case "low" -> "low";
            case "medium" -> "medium";
            case "xhigh", "max", "ultra" -> supportsXHighReasoning(model) ? "xhigh" : "high";
            default -> "high";
        };
    }

    private boolean supportsXHighReasoning(String model) {
        if (model.contains("codex-max")) return true;
        if (model.startsWith("gpt-6")) return true;
        if (!model.startsWith("gpt-5.")) return false;
        int versionStart = "gpt-5.".length();
        int versionEnd = versionStart;
        while (versionEnd < model.length() && Character.isDigit(model.charAt(versionEnd))) versionEnd++;
        if (versionEnd == versionStart) return false;
        try {
            return Integer.parseInt(model.substring(versionStart, versionEnd)) >= 2;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    /**
     * 构建单条 input 消息（Responses API 格式）
     */
    private Map<String, Object> buildInputMessage(LLMMessage source) {
        String role = source == null || source.getRole() == null || source.getRole().isBlank()
                ? "user" : source.getRole();
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);

        List<Map<String, Object>> content = new ArrayList<>();
        if (source != null && source.getContentParts() != null && !source.getContentParts().isEmpty()) {
            for (LLMContentPart part : source.getContentParts()) {
                content.add(buildInputContentPart(part));
            }
        } else if (source != null && source.getContent() != null) {
            content.add(buildInputTextPart(source.getContent()));
        }
        message.put("content", content);
        return message;
    }

    private Map<String, Object> buildInputMessage(String role, String text) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);

        message.put("content", List.of(buildInputTextPart(text)));

        return message;
    }

    private Map<String, Object> buildInputContentPart(LLMContentPart part) {
        if (part == null || part.getType() == null) {
            throw new IllegalArgumentException("ChatGPT 消息内容不能为空");
        }
        if (LLMContentPart.TYPE_TEXT.equals(part.getType())) {
            return buildInputTextPart(part.getText());
        }
        if (LLMContentPart.TYPE_IMAGE_URL.equals(part.getType()) && part.getImageUrl() != null) {
            Map<String, Object> imageContent = new LinkedHashMap<>();
            imageContent.put("type", "input_image");
            imageContent.put("image_url", part.getImageUrl().getUrl());
            if (part.getImageUrl().getDetail() != null && !part.getImageUrl().getDetail().isBlank()) {
                imageContent.put("detail", part.getImageUrl().getDetail());
            }
            return imageContent;
        }
        throw new IllegalArgumentException("不支持的 ChatGPT 消息内容类型：" + part.getType());
    }

    private Map<String, Object> buildInputTextPart(String text) {
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "input_text");
        textContent.put("text", text == null ? "" : text);
        return textContent;
    }

    /**
     * 解析 Responses API 非流式响应
     *
     * 响应格式：
     * {
     *   "id": "resp_xxx",
     *   "model": "gpt-5.4",
     *   "output": [{
     *     "type": "message",
     *     "role": "assistant",
     *     "content": [{ "type": "output_text", "text": "..." }]
     *   }],
     *   "usage": { "input_tokens": N, "output_tokens": N, "total_tokens": N }
     * }
     */
    private LLMResponse parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(true);

            if (jsonNode.has("id")) {
                response.setId(jsonNode.get("id").asText());
            }
            if (jsonNode.has("model")) {
                response.setModel(jsonNode.get("model").asText());
            }
            if (jsonNode.has("created_at")) {
                response.setCreated(jsonNode.get("created_at").asLong());
            }

            // 检查错误（跳过 "error": null 的情况）
            if (jsonNode.has("error") && !jsonNode.get("error").isNull()) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMsg = errorNode.isObject() && errorNode.has("message")
                        ? errorNode.get("message").asText()
                        : errorNode.asText();
                if (errorMsg != null && !"null".equals(errorMsg) && !errorMsg.isBlank()) {
                    response.setError(errorMsg);
                    return response;
                }
            }

            // 解析 output 数组，映射为 choices
            if (jsonNode.has("output")) {
                JsonNode outputArray = jsonNode.get("output");
                List<LLMChoice> choices = new ArrayList<>();
                StringBuilder reasoningContent = new StringBuilder();
                StringBuilder reasoningSummary = new StringBuilder();
                int index = 0;

                for (JsonNode outputItem : outputArray) {
                    String type = outputItem.has("type") ? outputItem.get("type").asText() : "";
                    if ("reasoning".equals(type)) {
                        appendReasoningParts(outputItem.get("content"), "reasoning_text", reasoningContent);
                        appendReasoningParts(outputItem.get("summary"), "summary_text", reasoningSummary);
                        continue;
                    }
                    if (!"message".equals(type)) {
                        continue;
                    }

                    LLMChoice choice = new LLMChoice();
                    choice.setIndex(index++);

                    // 从 content 数组提取文本
                    StringBuilder textBuilder = new StringBuilder();
                    if (outputItem.has("content")) {
                        for (JsonNode contentItem : outputItem.get("content")) {
                            String contentType = contentItem.has("type") ? contentItem.get("type").asText() : "";
                            if ("output_text".equals(contentType) && contentItem.has("text")) {
                                textBuilder.append(contentItem.get("text").asText());
                            }
                        }
                    }

                    LLMMessage message = new LLMMessage();
                    message.setRole(outputItem.has("role") ? outputItem.get("role").asText() : "assistant");
                    message.setContent(textBuilder.toString());
                    choice.setMessage(message);

                    String status = outputItem.has("status") ? outputItem.get("status").asText() : "completed";
                    choice.setFinishReason("completed".equals(status) ? "stop" : status);

                    choices.add(choice);
                }

                response.setChoices(choices);
                if (!reasoningContent.isEmpty()) {
                    putReasoning(response, reasoningContent.toString(), "content", false);
                } else if (!reasoningSummary.isEmpty()) {
                    putReasoning(response, reasoningSummary.toString(), "summary", false);
                }
            }

            // 解析 usage（Responses API 使用 input_tokens / output_tokens）
            if (jsonNode.has("usage")) {
                JsonNode usageNode = jsonNode.get("usage");
                LLMUsage usage = new LLMUsage();

                if (usageNode.has("input_tokens")) {
                    usage.setPromptTokens(usageNode.get("input_tokens").asInt());
                }
                if (usageNode.has("output_tokens")) {
                    usage.setCompletionTokens(usageNode.get("output_tokens").asInt());
                }
                if (usageNode.has("total_tokens")) {
                    usage.setTotalTokens(usageNode.get("total_tokens").asInt());
                } else {
                    usage.setTotalTokens(
                            (usage.getPromptTokens() != null ? usage.getPromptTokens() : 0)
                            + (usage.getCompletionTokens() != null ? usage.getCompletionTokens() : 0));
                }

                response.setUsage(usage);
                copyReasoningUsage(response, usageNode.path("output_tokens_details"));
            }

            return response;

        } catch (Exception e) {
            log.error("解析ChatGPT响应失败: type={}", e.getClass().getSimpleName());
            return createErrorResponse("响应解析失败: " + e.getMessage());
        }
    }

    private LLMResponse parseChatCompletionsResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(true);
            copyCommonResponseFields(jsonNode, response);
            String error = extractApiError(jsonNode);
            if (error != null) return response.setError(error);

            List<LLMChoice> choices = new ArrayList<>();
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
                for (JsonNode choiceNode : jsonNode.get("choices")) {
                    JsonNode messageNode = choiceNode.get("message");
                    LLMMessage message = new LLMMessage()
                            .setRole(messageNode != null && messageNode.has("role")
                                    ? messageNode.get("role").asText() : "assistant")
                            .setContent(extractChatContent(messageNode == null ? null : messageNode.get("content")));
                    choices.add(new LLMChoice()
                            .setIndex(choiceNode.has("index") ? choiceNode.get("index").asInt() : choices.size())
                            .setMessage(message)
                            .setFinishReason(textOrNull(choiceNode.get("finish_reason"))));
                    String reasoning = messageNode == null ? null
                            : textOrNull(messageNode.get("reasoning_content"));
                    if (reasoning != null) putReasoning(response, reasoning, "content", false);
                }
            }
            response.setChoices(choices);
            if (jsonNode.has("usage")) {
                response.setUsage(parseChatUsage(jsonNode.get("usage")));
                copyReasoningUsage(response, jsonNode.path("usage").path("completion_tokens_details"));
            }
            return response;
        } catch (Exception error) {
            log.error("解析 Chat Completions 响应失败: type={}", error.getClass().getSimpleName());
            return createErrorResponse("响应解析失败: " + error.getMessage());
        }
    }

    private LLMResponse parseChatCompletionsStreamResponse(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(false);
            copyCommonResponseFields(jsonNode, response);
            String error = extractApiError(jsonNode);
            if (error != null) return response.setError(error).setFinished(true);

            List<LLMChoice> choices = new ArrayList<>();
            if (jsonNode.has("choices") && jsonNode.get("choices").isArray()) {
                for (JsonNode choiceNode : jsonNode.get("choices")) {
                    JsonNode deltaNode = choiceNode.get("delta");
                    LLMMessage delta = new LLMMessage()
                            .setRole(deltaNode != null && deltaNode.has("role")
                                    ? deltaNode.get("role").asText() : "assistant")
                            .setContent(extractChatContent(deltaNode == null ? null : deltaNode.get("content")));
                    String finishReason = textOrNull(choiceNode.get("finish_reason"));
                    choices.add(new LLMChoice()
                            .setIndex(choiceNode.has("index") ? choiceNode.get("index").asInt() : choices.size())
                            .setDelta(delta)
                            .setFinishReason(finishReason));
                    if (finishReason != null) response.setFinished(true);

                    if (deltaNode != null && deltaNode.has("reasoning_content")
                            && !deltaNode.get("reasoning_content").isNull()) {
                        putReasoning(response, deltaNode.get("reasoning_content").asText(), "content", true);
                    }
                }
            }
            response.setChoices(choices);
            if (jsonNode.has("usage") && !jsonNode.get("usage").isNull()) {
                response.setUsage(parseChatUsage(jsonNode.get("usage")));
            }
            return response;
        } catch (Exception error) {
            log.error("解析 Chat Completions 流式响应失败: type={}", error.getClass().getSimpleName());
            return null;
        }
    }

    private void copyCommonResponseFields(JsonNode source, LLMResponse target) {
        if (source.has("id")) target.setId(source.get("id").asText());
        if (source.has("model")) target.setModel(source.get("model").asText());
        if (source.has("created")) target.setCreated(source.get("created").asLong());
        if (source.has("created_at")) target.setCreated(source.get("created_at").asLong());
    }

    private String extractApiError(JsonNode jsonNode) {
        if (!jsonNode.has("error") || jsonNode.get("error").isNull()) return null;
        JsonNode errorNode = jsonNode.get("error");
        String message = errorNode.isObject() && errorNode.has("message")
                ? errorNode.get("message").asText() : errorNode.asText();
        return message == null || message.isBlank() || "null".equals(message) ? null : message;
    }

    private String extractChatContent(JsonNode contentNode) {
        if (contentNode == null || contentNode.isNull()) return "";
        if (contentNode.isTextual()) return contentNode.asText();
        if (!contentNode.isArray()) return contentNode.asText("");
        StringBuilder text = new StringBuilder();
        for (JsonNode item : contentNode) {
            if (item.has("text")) text.append(item.get("text").asText());
        }
        return text.toString();
    }

    private void copyReasoningUsage(LLMResponse response, JsonNode details) {
        JsonNode tokens = details.path("reasoning_tokens");
        if (tokens.isIntegralNumber() && tokens.canConvertToLong() && tokens.longValue() >= 0) {
            if (response.getExtraData() == null) response.setExtraData(new LinkedHashMap<>());
            response.getExtraData().put("reasoning_tokens", tokens.longValue());
        }
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() || node.asText().isBlank() ? null : node.asText();
    }

    private void appendReasoningParts(JsonNode parts, String expectedType, StringBuilder output) {
        if (parts == null || !parts.isArray()) return;
        for (JsonNode part : parts) {
            String type = textOrNull(part.get("type"));
            String text = textOrNull(part.get("text"));
            if (text != null && (type == null || expectedType.equals(type))) output.append(text);
        }
    }

    private void putReasoning(LLMResponse response, String reasoning, String type, boolean delta) {
        if (reasoning == null || reasoning.isEmpty()) return;
        if (response.getExtraData() == null) response.setExtraData(new LinkedHashMap<>());
        response.getExtraData().put(delta ? "reasoning_content_delta" : "reasoning_content", reasoning);
        response.getExtraData().put("reasoning_type", type);
    }

    private LLMUsage parseChatUsage(JsonNode usageNode) {
        LLMUsage usage = new LLMUsage();
        if (usageNode.has("prompt_tokens")) usage.setPromptTokens(usageNode.get("prompt_tokens").asInt());
        if (usageNode.has("completion_tokens")) usage.setCompletionTokens(usageNode.get("completion_tokens").asInt());
        if (usageNode.has("total_tokens")) usage.setTotalTokens(usageNode.get("total_tokens").asInt());
        return usage;
    }

    /**
     * 解析流式响应事件
     */
    private LLMResponse parseStreamResponse(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);

            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(false);

            if (jsonNode.has("id")) {
                response.setId(jsonNode.get("id").asText());
            }
            if (jsonNode.has("model")) {
                response.setModel(jsonNode.get("model").asText());
            }

            // 流式事件中的 type 字段
            String eventType = jsonNode.has("type") ? jsonNode.get("type").asText() : "";

            if (eventType.contains("reasoning") && eventType.endsWith(".delta")) {
                String delta = jsonNode.has("delta") ? jsonNode.get("delta").asText() : "";
                if (delta.isBlank() && jsonNode.has("text")) delta = jsonNode.get("text").asText();
                if (!delta.isBlank()) {
                    String reasoningType = eventType.contains("reasoning_summary") ? "summary" : "content";
                    putReasoning(response, delta, reasoningType, true);
                }
            } else if ("response.output_text.delta".equals(eventType)) {
                // 文本增量
                String delta = jsonNode.has("delta") ? jsonNode.get("delta").asText() : "";
                LLMChoice choice = new LLMChoice();
                choice.setIndex(0);
                LLMMessage deltaMsg = new LLMMessage();
                deltaMsg.setRole("assistant");
                deltaMsg.setContent(delta);
                choice.setDelta(deltaMsg);
                response.setChoices(List.of(choice));

            } else if ("response.completed".equals(eventType) || "response.done".equals(eventType)) {
                response.setFinished(true);
                // 尝试从完成事件提取 usage
                if (jsonNode.has("response") && jsonNode.get("response").has("usage")) {
                    JsonNode usageNode = jsonNode.get("response").get("usage");
                    LLMUsage usage = new LLMUsage();
                    if (usageNode.has("input_tokens")) {
                        usage.setPromptTokens(usageNode.get("input_tokens").asInt());
                    }
                    if (usageNode.has("output_tokens")) {
                        usage.setCompletionTokens(usageNode.get("output_tokens").asInt());
                    }
                    response.setUsage(usage);
                }
            }

            return response;

        } catch (Exception e) {
            log.error("解析ChatGPT流式响应失败: type={}", e.getClass().getSimpleName());
            return null;
        }
    }

    List<String> extractStreamPayloads(String chunk) {
        if (chunk == null || chunk.isBlank()) {
            return List.of();
        }
        List<String> payloads = new ArrayList<>();
        for (String rawLine : chunk.replace("\r", "").split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith(":") || line.startsWith("event:")
                    || line.startsWith("id:") || line.startsWith("retry:")) {
                continue;
            }
            if (line.startsWith("data:")) {
                line = line.substring(5).trim();
            }
            if (!line.isEmpty() && !"[DONE]".equalsIgnoreCase(line)) {
                payloads.add(line);
            }
        }
        return payloads;
    }

    private LLMResponse createErrorResponse(String errorMessage) {
        return new LLMResponse()
                .setProvider(getProvider())
                .setError(errorMessage)
                .setFinished(true);
    }

    private enum ApiProtocol {
        RESPONSES,
        CHAT_COMPLETIONS
    }

    private record ApiTarget(ApiProtocol protocol, String url) {}

    private record ApiPlan(ApiTarget primary, ApiTarget fallback) {}

    private static final class UpstreamApiException extends RuntimeException {
        private final int status;

        private UpstreamApiException(int status, String message) {
            super(message);
            this.status = status;
        }

        private int status() {
            return status;
        }
    }
}
