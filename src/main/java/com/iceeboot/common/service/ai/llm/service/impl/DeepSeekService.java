package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.config.LlmModelRoutingProperties;
import com.iceeboot.common.service.ai.llm.dto.*;
import com.iceeboot.common.service.ai.llm.security.PublicEndpointWebClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DeepSeek大模型服务实现类
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class DeepSeekService implements com.iceeboot.common.service.ai.llm.service.LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://api.deepseek.com";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String REASONING_MODEL = "deepseek-reasoner";
    private static final Integer DEFAULT_TIMEOUT = 60;
    private static final Integer DEFAULT_RETRY_MAX = 2;
    private static final Integer DEFAULT_RETRY_BACKOFF_MS = 800;
    
    private final WebClient webClient;
    private final WebClient publicEndpointWebClient;
    private final ObjectMapper objectMapper;
    private LlmModelRoutingProperties modelRoutingProperties = new LlmModelRoutingProperties();

    public DeepSeekService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.publicEndpointWebClient = PublicEndpointWebClientFactory.create(10 * 1024 * 1024);
        this.objectMapper = new ObjectMapper();
    }

    @Autowired
    void setModelRoutingProperties(LlmModelRoutingProperties modelRoutingProperties) {
        if (modelRoutingProperties != null) {
            this.modelRoutingProperties = modelRoutingProperties;
        }
    }
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.DEEPSEEK;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        return buildRequestBody(request, false)
                .flatMap(requestBody -> {
                    String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                    String endpoint = buildChatEndpoint(baseUrl);
                    
                    return webClient(request).post()
                            .uri(endpoint)
                            .header("Authorization", "Bearer " + request.getApiKey())
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(responseBody -> {
                                        if (clientResponse.statusCode().is2xxSuccessful()) {
                                            return parseResponse(responseBody);
                                        }
                                        String errorBody = summarizeError(responseBody);
                                        log.error("DeepSeek API调用失败: status={}", clientResponse.statusCode().value());
                                        return createErrorResponse("API调用失败（HTTP "
                                                + clientResponse.statusCode().value() + "）：" + errorBody);
                                    }))
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .doOnError(error -> log.error(
                                    "DeepSeek API调用异常: type={}", error.getClass().getSimpleName()))
                            .onErrorResume(error -> Mono.just(createErrorResponse(
                                    "API调用失败：" + summarizeError(error.getMessage()))));
                });
    }

    private boolean isDashScopeBaseUrl(String baseUrl) {
        if (!StringUtils.hasText(baseUrl)) {
            return false;
        }
        String normalized = baseUrl.trim().toLowerCase(Locale.ROOT);
        return normalized.contains("dashscope.aliyuncs.com");
    }

    private boolean resolveThinkingEnabled(LLMRequest request) {
        if (request == null) {
            return false;
        }
        if (request.getThinking() != null) {
            return request.getThinking();
        }
        if (request.getExtraParams() == null) {
            return false;
        }
        Object thinking = request.getExtraParams().get("thinking");
        if (thinking instanceof Map<?, ?> map) {
            Object type = map.get("type");
            return type != null && "enabled".equalsIgnoreCase(String.valueOf(type).trim());
        }
        return thinking instanceof Boolean enabled && enabled;
    }

    private String buildChatEndpoint(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized.endsWith(CHAT_ENDPOINT) ? normalized : normalized + CHAT_ENDPOINT;
    }

    private WebClient webClient(LLMRequest request) {
        return Boolean.TRUE.equals(request.getPublicEndpointOnly()) ? publicEndpointWebClient : webClient;
    }

    private String sanitizeBaseUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : DEFAULT_BASE_URL;
        try {
            URI uri = URI.create(normalized);
            String port = uri.getPort() >= 0 ? ":" + uri.getPort() : "";
            String path = uri.getPath() == null ? "" : uri.getPath();
            return uri.getScheme() + "://" + uri.getHost() + port + path;
        } catch (Exception ignored) {
            return "(invalid-url)";
        }
    }

    private String summarizeError(String message) {
        if (!StringUtils.hasText(message)) {
            return "上游未返回错误详情";
        }
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.length() > 1000 ? normalized.substring(0, 1000) + "..." : normalized;
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        return buildRequestBody(request, true)
                .flatMapMany(requestBody -> {
                    String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                    AtomicBoolean emittedText = new AtomicBoolean(false);
                    
                    return webClient(request).post()
                            .uri(buildChatEndpoint(baseUrl))
                            .header("Authorization", "Bearer " + request.getApiKey())
                            .header("Content-Type", "application/json")
                            .header("Accept", "text/event-stream")
                            .bodyValue(requestBody)
                            .exchangeToFlux(response -> response.statusCode().is2xxSuccessful()
                                    ? response.bodyToFlux(String.class)
                                    : response.createException().flatMapMany(Flux::error))
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .concatMapIterable(this::extractStreamPayloads)
                            .concatMap(data -> {
                                LLMResponse parsed = parseStreamResponse(data);
                                return parsed == null ? Mono.empty() : Mono.just(parsed);
                            })
                            .doOnNext(response -> {
                                if (hasResponseContent(response)) {
                                    emittedText.set(true);
                                }
                            })
                            .retryWhen(Retry.backoff(DEFAULT_RETRY_MAX, Duration.ofMillis(DEFAULT_RETRY_BACKOFF_MS))
                                    .filter(error -> !emittedText.get() && isRetryable(error)))
                            .doOnError(error -> log.error("DeepSeek流式API调用失败: type={}", error.getClass().getSimpleName()))
                            .onErrorResume(error -> Flux.just(createErrorResponse(
                                    "流式API调用失败：" + streamErrorMessage(error, request.getApiKey()))));
                });
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
        // DeepSeek支持的模型列表
        List<String> models = Arrays.asList(
                "deepseek-chat",
                "deepseek-reasoner"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        // DeepSeek模型的最大token限制
        if (REASONING_MODEL.equals(model)) {
            return 32768; // 推理模型支持更长的上下文
        }
        return 32768;
    }
    
    /**
     * 构建请求体
     */
    private Mono<Map<String, Object>> buildRequestBody(LLMRequest request, boolean stream) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            
            // 确定使用的模型
            String model = normalizeModel(request.getModel());
            if (model == null || model.trim().isEmpty()) {
                model = DEFAULT_MODEL;
            }
            
            // 检查是否需要深度思考功能
            boolean needReasoning = isReasoningRequired(request);
            if (needReasoning && !REASONING_MODEL.equals(model)
                    && !Boolean.TRUE.equals(request.getPublicEndpointOnly())) {
                model = REASONING_MODEL;
            }

            if (Boolean.FALSE.equals(request.getThinking()) && REASONING_MODEL.equalsIgnoreCase(model)) {
                throw new IllegalArgumentException("所选 DeepSeek Reasoner 模型不支持关闭思考");
            }
            
            requestBody.put("model", model);
            requestBody.put("stream", stream);
            
            // 构建消息列表
            List<Map<String, Object>> messages = new ArrayList<>();
            
            // 添加系统提示词
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
                Map<String, Object> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", request.getSystemPrompt());
                messages.add(systemMessage);
            }
            
            // 添加历史消息
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                for (LLMMessage msg : request.getMessages()) {
                    Map<String, Object> message = new HashMap<>();
                    message.put("role", msg.getRole());
                    message.put("content", messageContent(msg));
                    messages.add(message);
                }
            } else if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
                // 添加用户消息
                Map<String, Object> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                userMessage.put("content", request.getUserMessage());
                messages.add(userMessage);
            }
            
            requestBody.put("messages", messages);
            
            // 设置其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_tokens", request.getMaxTokens());
            }
            
            // 添加扩展参数
            if (request.getExtraParams() != null) {
                requestBody.putAll(request.getExtraParams());
            }
            if (REASONING_MODEL.equalsIgnoreCase(model)) {
                // deepseek-reasoner 通过模型名固定开启思考，不接受新版 V4 的开关和强度参数。
                requestBody.remove("thinking");
                requestBody.remove("reasoning_effort");
            } else if (isDashScopeBaseUrl(request.getBaseUrl())) {
                requestBody.remove("thinking");
                requestBody.remove("reasoning_effort");
                requestBody.put("enable_thinking", resolveThinkingEnabled(request));
            } else if (request.getThinking() != null || supportsThinkingToggle(model)) {
                requestBody.put("thinking", Map.of(
                        "type", resolveThinkingEnabled(request) ? "enabled" : "disabled"));
                if (resolveThinkingEnabled(request)) {
                    requestBody.put("reasoning_effort", deepSeekReasoningEffort(request.getReasoningEffort()));
                } else {
                    requestBody.remove("reasoning_effort");
                }
            }
            if (stream && !requestBody.containsKey("stream_options")) {
                requestBody.put("stream_options", Map.of("include_usage", true));
            }
            
            return requestBody;
        });
    }

    private Object messageContent(LLMMessage message) {
        if (message.getContentParts() == null || message.getContentParts().isEmpty()) {
            return message.getContent();
        }
        return message.getContentParts().stream().map(this::contentPart).toList();
    }

    private Map<String, Object> contentPart(LLMContentPart part) {
        if (LLMContentPart.TYPE_TEXT.equals(part.getType())) {
            return Map.of("type", LLMContentPart.TYPE_TEXT, "text", part.getText());
        }
        if (LLMContentPart.TYPE_IMAGE_URL.equals(part.getType()) && part.getImageUrl() != null) {
            Map<String, Object> imageUrl = new LinkedHashMap<>();
            imageUrl.put("url", part.getImageUrl().getUrl());
            if (StringUtils.hasText(part.getImageUrl().getDetail())) {
                imageUrl.put("detail", part.getImageUrl().getDetail());
            }
            return Map.of("type", LLMContentPart.TYPE_IMAGE_URL, "image_url", imageUrl);
        }
        throw new IllegalArgumentException("不支持的 DeepSeek 消息内容类型：" + part.getType());
    }

    private String normalizeModel(String model) {
        return modelRoutingProperties.normalize(SystemConstants.LLMProvider.DEEPSEEK, model);
    }
    
    /**
     * 判断是否需要推理功能
     */
    private boolean isReasoningRequired(LLMRequest request) {
        if (request == null || Boolean.FALSE.equals(request.getThinking())) return false;
        if (Boolean.TRUE.equals(request.getPublicEndpointOnly())) return false;
        // 检查扩展参数中是否指定了推理模式
        if (request.getExtraParams() != null) {
            Object reasoning = request.getExtraParams().get("reasoning");
            if (reasoning instanceof Boolean) {
                return (Boolean) reasoning;
            }
        }
        
        // 检查模型名称是否包含推理相关关键词
        String model = request.getModel();
        if (model != null) {
            return model.contains("reasoner") || model.contains("reasoning") || model.contains("think");
        }
        
        return false;
    }

    private boolean supportsThinkingToggle(String model) {
        if (!StringUtils.hasText(model)) return false;
        String normalized = model.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith("deepseek-v4") || normalized.startsWith("deepseek-flash");
    }

    private String deepSeekReasoningEffort(String value) {
        if (!StringUtils.hasText(value)) return "high";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "minimal", "low" -> "low";
            case "medium", "high" -> "high";
            case "xhigh", "max", "ultra" -> "max";
            default -> "high";
        };
    }

    private String streamErrorMessage(Throwable error, String apiKey) {
        String message;
        if (error instanceof WebClientResponseException upstream) {
            String body = summarizeError(upstream.getResponseBodyAsString());
            message = "API调用失败（HTTP " + upstream.getStatusCode().value() + "）：" + body;
        } else {
            message = summarizeError(error == null ? null : error.getMessage());
        }
        return StringUtils.hasText(apiKey) ? message.replace(apiKey, "***") : message;
    }
    
    /**
     * 解析非流式响应
     */
    private LLMResponse parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(true);
            
            // 解析基本信息
            String id = readNullableText(jsonNode, "id");
            if (id != null) {
                response.setId(id);
            }
            String model = readNullableText(jsonNode, "model");
            if (model != null) {
                response.setModel(model);
            }
            if (jsonNode.has("created")) {
                response.setCreated(jsonNode.get("created").asLong());
            }
            
            // 解析选择列表
            if (jsonNode.has("choices")) {
                JsonNode choicesNode = jsonNode.get("choices");
                List<LLMChoice> choices = new ArrayList<>();
                
                for (JsonNode choiceNode : choicesNode) {
                    LLMChoice choice = new LLMChoice();
                    
                    if (choiceNode.has("index")) {
                        choice.setIndex(choiceNode.get("index").asInt());
                    }
                    
                    String finishReason = readNullableText(choiceNode, "finish_reason");
                    if (finishReason != null) {
                        choice.setFinishReason(finishReason);
                    }
                    
                    // 解析消息内容
                    if (choiceNode.has("message")) {
                        JsonNode messageNode = choiceNode.get("message");
                        LLMMessage message = new LLMMessage();
                        
                        String role = readNullableText(messageNode, "role");
                        if (role != null) {
                            message.setRole(role);
                        }
                        String content = readNullableText(messageNode, "content");
                        if (content != null) {
                            message.setContent(content);
                        }
                        
                        choice.setMessage(message);
                        
                        // 处理推理内容（DeepSeek特有）
                        String reasoningContent = readNullableText(messageNode, "reasoning_content");
                        if (reasoningContent != null) {
                            if (response.getExtraData() == null) {
                                response.setExtraData(new HashMap<>());
                            }
                            response.getExtraData().put("reasoning_content", reasoningContent);
                            response.getExtraData().put("reasoning_type", "content");
                        }
                    }
                    
                    choices.add(choice);
                }
                
                response.setChoices(choices);
            }
            
            // 解析使用情况
            if (jsonNode.has("usage")) {
                JsonNode usageNode = jsonNode.get("usage");
                LLMUsage usage = new LLMUsage();
                
                if (usageNode.has("prompt_tokens")) {
                    usage.setPromptTokens(usageNode.get("prompt_tokens").asInt());
                }
                if (usageNode.has("completion_tokens")) {
                    usage.setCompletionTokens(usageNode.get("completion_tokens").asInt());
                }
                if (usageNode.has("total_tokens")) {
                    usage.setTotalTokens(usageNode.get("total_tokens").asInt());
                }
                
                response.setUsage(usage);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("解析DeepSeek响应失败: type={}", e.getClass().getSimpleName());
            return createErrorResponse("响应解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析流式响应
     */
    private LLMResponse parseStreamResponse(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(false);
            
            // 解析基本信息
            String id = readNullableText(jsonNode, "id");
            if (id != null) {
                response.setId(id);
            }
            String model = readNullableText(jsonNode, "model");
            if (model != null) {
                response.setModel(model);
            }
            if (jsonNode.has("created")) {
                response.setCreated(jsonNode.get("created").asLong());
            }
            
            // 解析选择列表
            if (jsonNode.has("choices")) {
                JsonNode choicesNode = jsonNode.get("choices");
                List<LLMChoice> choices = new ArrayList<>();
                
                for (JsonNode choiceNode : choicesNode) {
                    LLMChoice choice = new LLMChoice();
                    
                    if (choiceNode.has("index")) {
                        choice.setIndex(choiceNode.get("index").asInt());
                    }
                    
                    String finishReason = readNullableText(choiceNode, "finish_reason");
                    if (finishReason != null) {
                        choice.setFinishReason(finishReason);
                        response.setFinished(true);
                    }
                    
                    // 解析增量内容
                    if (choiceNode.has("delta")) {
                        JsonNode deltaNode = choiceNode.get("delta");
                        LLMMessage delta = new LLMMessage();
                        
                        String role = readNullableText(deltaNode, "role");
                        if (role != null) {
                            delta.setRole(role);
                        }
                        String content = readContentText(deltaNode);
                        if (content != null) {
                            delta.setContent(content);
                        }
                        
                        choice.setDelta(delta);
                        
                        // 处理推理内容增量（DeepSeek特有）
                        String reasoningContent = readNullableText(deltaNode, "reasoning_content");
                        if (reasoningContent != null) {
                            if (response.getExtraData() == null) {
                                response.setExtraData(new HashMap<>());
                            }
                            response.getExtraData().put("reasoning_content_delta", reasoningContent);
                            response.getExtraData().put("reasoning_type", "content");
                        }
                    }

                    if (choice.getDelta() == null && choiceNode.has("message")) {
                        JsonNode messageNode = choiceNode.get("message");
                        LLMMessage message = new LLMMessage();
                        message.setRole(readNullableText(messageNode, "role"));
                        message.setContent(readContentText(messageNode));
                        choice.setMessage(message);
                    }

                    if (choice.getDelta() == null && choice.getMessage() == null) {
                        String content = readContentText(choiceNode);
                        if (content != null) {
                            choice.setDelta(LLMMessage.assistant(content));
                        }
                    }
                    
                    choices.add(choice);
                }
                
                response.setChoices(choices);
            }

            if ((response.getChoices() == null || response.getChoices().isEmpty())) {
                String content = readContentText(jsonNode);
                if (content != null) {
                    response.setChoices(List.of(new LLMChoice()
                            .setIndex(0)
                            .setDelta(LLMMessage.assistant(content))));
                }
            }

            parseUsageNode(jsonNode, response);
            
            return response;
            
        } catch (Exception e) {
            log.error("解析DeepSeek流式响应失败: type={}", e.getClass().getSimpleName());
            return null; // 流式响应解析失败时返回null，会被过滤掉
        }
    }
    
    /**
     * 创建错误响应
     */
    private LLMResponse createErrorResponse(String errorMessage) {
        return new LLMResponse()
                .setProvider(getProvider())
                .setError(errorMessage)
                .setFinished(true);
    }

    private void parseUsageNode(JsonNode jsonNode, LLMResponse response) {
        if (jsonNode == null || response == null || !jsonNode.has("usage")) {
            return;
        }
        JsonNode usageNode = jsonNode.get("usage");
        if (usageNode == null || usageNode.isNull()) {
            return;
        }
        LLMUsage usage = new LLMUsage();
        if (usageNode.has("prompt_tokens")) {
            usage.setPromptTokens(usageNode.get("prompt_tokens").asInt());
        } else if (usageNode.has("input_tokens")) {
            usage.setPromptTokens(usageNode.get("input_tokens").asInt());
        }
        if (usageNode.has("completion_tokens")) {
            usage.setCompletionTokens(usageNode.get("completion_tokens").asInt());
        } else if (usageNode.has("output_tokens")) {
            usage.setCompletionTokens(usageNode.get("output_tokens").asInt());
        }
        if (usageNode.has("total_tokens")) {
            usage.setTotalTokens(usageNode.get("total_tokens").asInt());
        }
        response.setUsage(usage);
    }

    List<String> extractStreamPayloads(String chunk) {
        if (!StringUtils.hasText(chunk)) {
            return List.of();
        }
        List<String> payloads = new ArrayList<>();
        for (String rawLine : chunk.replace("\r", "").split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith(":")
                    || line.startsWith("event:") || line.startsWith("id:") || line.startsWith("retry:")) {
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

    private String readContentText(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isTextual()) {
            return normalizeNullableText(node.asText());
        }
        for (String fieldName : List.of("content", "output_text", "text", "delta")) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isTextual()) {
                String text = normalizeNullableText(value.asText());
                if (text != null) {
                    return text;
                }
            }
            if (value.isArray()) {
                StringBuilder content = new StringBuilder();
                for (JsonNode part : value) {
                    String text = readContentText(part);
                    if (text != null) {
                        content.append(text);
                    }
                }
                if (!content.isEmpty()) {
                    return content.toString();
                }
            }
        }
        return null;
    }

    private String readNullableText(JsonNode node, String fieldName) {
        if (node == null || fieldName == null || !node.has(fieldName)) {
            return null;
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return null;
        }
        return normalizeNullableText(fieldNode.asText());
    }

    private String normalizeNullableText(String text) {
        if (text == null || "null".equalsIgnoreCase(text)) {
            return null;
        }
        return text;
    }

    private boolean hasResponseContent(LLMResponse response) {
        if (response == null || response.getChoices() == null) {
            return false;
        }
        for (LLMChoice choice : response.getChoices()) {
            if (choice == null) {
                continue;
            }
            if (choice.getDelta() != null && StringUtils.hasText(choice.getDelta().getContent())) {
                return true;
            }
            if (choice.getMessage() != null && StringUtils.hasText(choice.getMessage().getContent())) {
                return true;
            }
        }
        return false;
    }

    private boolean isRetryable(Throwable error) {
        return error instanceof IOException
                || error instanceof TimeoutException
                || error instanceof UnknownHostException
                || error instanceof WebClientResponseException upstream
                && upstream.getStatusCode().is5xxServerError();
    }
}
