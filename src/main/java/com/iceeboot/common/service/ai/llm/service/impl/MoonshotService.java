package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.*;
import com.iceeboot.common.service.ai.llm.security.PublicEndpointWebClientFactory;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Kimi（月之暗面）大模型服务实现类
 */
@Slf4j
@Component
public class MoonshotService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://api.moonshot.cn/v1";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    private static final String DEFAULT_MODEL = "moonshot-v1-8k";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    private final WebClient publicEndpointWebClient;
    private final ObjectMapper objectMapper;
    
    public MoonshotService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.publicEndpointWebClient = PublicEndpointWebClientFactory.create(10 * 1024 * 1024);
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.MOONSHOT;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        return buildRequestBody(request, false)
                .flatMap(requestBody -> {
                    String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                    String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
                    
                    String endpoint = buildChatEndpoint(baseUrl);
                    return webClient(request).post()
                            .uri(endpoint)
                            .header("Authorization", "Bearer " + request.getApiKey())
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .exchangeToMono(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToMono(String.class);
                                }
                                return response.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMap(errorBody -> {
                                            log.error("Kimi API返回错误: status={}", response.statusCode());
                                            return Mono.error(new RuntimeException(
                                                    "API返回" + response.statusCode() + ": " + errorBody));
                                        });
                            })
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .map(this::parseResponse)
                            .doOnError(error -> log.error("Kimi API调用失败: type={}", error.getClass().getSimpleName()))
                            .onErrorResume(error -> Mono.just(createErrorResponse(
                                    "API调用失败：" + safeErrorMessage(error.getMessage()))));
                });
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        return buildRequestBody(request, true)
                .flatMapMany(requestBody -> {
                    String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                    String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
                    
                    String endpoint = buildChatEndpoint(baseUrl);
                    return webClient(request).post()
                            .uri(endpoint)
                            .header("Authorization", "Bearer " + request.getApiKey())
                            .header("Content-Type", "application/json")
                            .header("Accept", "text/event-stream")
                            .bodyValue(requestBody)
                            .exchangeToFlux(response -> {
                                if (response.statusCode().is2xxSuccessful()) {
                                    return response.bodyToFlux(String.class);
                                }
                                return response.bodyToMono(String.class)
                                        .defaultIfEmpty("")
                                        .flatMapMany(errorBody -> {
                                            log.error("Kimi流式API返回错误: status={}", response.statusCode());
                                            return Flux.error(new RuntimeException(
                                                    "API返回" + response.statusCode() + ": " + errorBody));
                                        });
                            })
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .concatMapIterable(this::extractStreamPayloads)
                            .map(this::parseStreamResponse)
                            .filter(Objects::nonNull)
                            .doOnError(error -> log.error("Kimi流式API调用失败: type={}", error.getClass().getSimpleName()))
                            .onErrorResume(error -> Flux.just(createErrorResponse(
                                    "流式API调用失败：" + safeErrorMessage(error.getMessage()))));
                });
    }

    private String buildChatEndpoint(String configuredBaseUrl) {
        String baseUrl = configuredBaseUrl == null || configuredBaseUrl.isBlank()
                ? DEFAULT_BASE_URL : configuredBaseUrl.trim();
        while (baseUrl.endsWith("/")) baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        return baseUrl.endsWith(CHAT_ENDPOINT) ? baseUrl : baseUrl + CHAT_ENDPOINT;
    }

    private WebClient webClient(LLMRequest request) {
        return Boolean.TRUE.equals(request.getPublicEndpointOnly()) ? publicEndpointWebClient : webClient;
    }

    private String safeErrorMessage(String message) {
        String normalized = message == null || message.isBlank() ? "未知错误" : message.replaceAll("\\s+", " ").trim();
        return normalized.length() > 300 ? normalized.substring(0, 300) + "..." : normalized;
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
        // Kimi支持的模型列表 <mcreference link="https://github.com/LLM-Red-Team/kimi-free-api" index="2">2</mcreference>
        List<String> models = Arrays.asList(
                "moonshot-v1-8k",
                "moonshot-v1-32k",
                "moonshot-v1-128k"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        if (model != null) {
            if (model.contains("128k")) {
                return 128000;
            } else if (model.contains("32k")) {
                return 32000;
            }
        }
        return 8000;
    }
    
    /**
     * 构建请求体
     */
    private Mono<Map<String, Object>> buildRequestBody(LLMRequest request, boolean stream) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            
            // 确定使用的模型
            String model = request.getModel();
            if (model == null || model.trim().isEmpty()) {
                model = DEFAULT_MODEL;
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
            applyThinkingMode(requestBody, request, model);
            
            return requestBody;
        });
    }

    private void applyThinkingMode(Map<String, Object> requestBody, LLMRequest request, String model) {
        if (request.getThinking() == null || model == null) return;
        String normalizedModel = model.trim().toLowerCase(Locale.ROOT);
        // K2.5/K2.6 是混合思考模型，仅支持开关，不接受 reasoning_effort。
        if (normalizedModel.startsWith("kimi-k2.5") || normalizedModel.startsWith("kimi-k2.6")) {
            requestBody.put("thinking", Map.of("type", request.getThinking() ? "enabled" : "disabled"));
            return;
        }
        boolean alwaysThinking = normalizedModel.startsWith("kimi-k3")
                || normalizedModel.startsWith("kimi-k2.7-code")
                || normalizedModel.contains("thinking");
        if (alwaysThinking && !request.getThinking()) {
            throw new IllegalArgumentException("所选 Kimi 模型固定开启思考，普通请求和普通截图无法使用；不支持关闭思考可以调用");
        }
        if (normalizedModel.startsWith("kimi-k3") && request.getThinking()) {
            requestBody.put("reasoning_effort", kimiReasoningEffort(request.getReasoningEffort()));
        }
    }

    private String kimiReasoningEffort(String value) {
        if (!StringUtils.hasText(value)) return "high";
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "minimal", "low" -> "low";
            case "medium", "high" -> "high";
            case "xhigh", "max", "ultra" -> "max";
            default -> "high";
        };
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
            Map<String, Object> imageUrl = new LinkedHashMap<>();
            imageUrl.put("url", part.getImageUrl().getUrl());
            if (part.getImageUrl().getDetail() != null && !part.getImageUrl().getDetail().isBlank()) {
                imageUrl.put("detail", part.getImageUrl().getDetail());
            }
            return Map.of("type", "image_url", "image_url", imageUrl);
        }
        throw new IllegalArgumentException("不支持的 Kimi 消息内容类型：" + part.getType());
    }
    
    /**
     * 解析非流式响应
     */
    private LLMResponse parseResponse(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
            // 检查是否有错误信息
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMessage = "API调用失败";
                if (errorNode.has("message")) {
                    errorMessage = errorNode.get("message").asText();
                } else if (errorNode.has("code")) {
                    errorMessage = "错误代码: " + errorNode.get("code").asText();
                }
                log.error("Kimi API返回业务错误");
                return createErrorResponse(errorMessage);
            }
            
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(true);
            
            // 解析基本信息
            if (jsonNode.has("id")) {
                response.setId(jsonNode.get("id").asText());
            }
            if (jsonNode.has("model")) {
                response.setModel(jsonNode.get("model").asText());
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
                    
                    if (choiceNode.has("finish_reason")) {
                        choice.setFinishReason(choiceNode.get("finish_reason").asText());
                    }
                    
                    // 解析消息内容
                    if (choiceNode.has("message")) {
                        JsonNode messageNode = choiceNode.get("message");
                        LLMMessage message = new LLMMessage();
                        
                        if (messageNode.has("role")) {
                            message.setRole(messageNode.get("role").asText());
                        }
                        if (messageNode.has("content")) {
                            message.setContent(messageNode.get("content").asText());
                        }
                        String reasoning = nullableText(messageNode, "reasoning_content");
                        if (reasoning != null) {
                            if (response.getExtraData() == null) response.setExtraData(new LinkedHashMap<>());
                            response.getExtraData().put("reasoning_content", reasoning);
                            response.getExtraData().put("reasoning_type", "content");
                        }
                        
                        choice.setMessage(message);
                    }
                    
                    choices.add(choice);
                }
                
                response.setChoices(choices);
            } else {
                log.warn("Kimi响应中缺少choices字段");
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
            
            // 验证响应完整性
            if (response.getChoices() == null || response.getChoices().isEmpty()) {
                log.warn("Kimi响应解析完成但choices为空，可能是响应格式异常");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("解析Kimi响应失败: type={}", e.getClass().getSimpleName());
            return createErrorResponse("响应解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析流式响应
     */
    private LLMResponse parseStreamResponse(String data) {
        try {
            JsonNode jsonNode = objectMapper.readTree(data);
            
            // 检查是否有错误信息
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMessage = "流式API调用失败";
                if (errorNode.has("message")) {
                    errorMessage = errorNode.get("message").asText();
                }
                log.error("Kimi流式API返回业务错误");
                return createErrorResponse(errorMessage);
            }
            
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(false);
            
            // 解析基本信息
            if (jsonNode.has("id")) {
                response.setId(jsonNode.get("id").asText());
            }
            if (jsonNode.has("model")) {
                response.setModel(jsonNode.get("model").asText());
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
                    
                    if (choiceNode.has("finish_reason")) {
                        String finishReason = choiceNode.get("finish_reason").asText();
                        choice.setFinishReason(finishReason);
                        if (!finishReason.isBlank() && !"null".equalsIgnoreCase(finishReason)) {
                            response.setFinished(true);
                        }
                    }
                    
                    // 解析增量内容
                    if (choiceNode.has("delta")) {
                        JsonNode deltaNode = choiceNode.get("delta");
                        LLMMessage delta = new LLMMessage();
                        
                        if (deltaNode.has("role")) {
                            delta.setRole(deltaNode.get("role").asText());
                        }
                        if (deltaNode.has("content")) {
                            delta.setContent(deltaNode.get("content").asText());
                        }
                        String reasoning = nullableText(deltaNode, "reasoning_content");
                        if (reasoning != null) {
                            if (response.getExtraData() == null) response.setExtraData(new LinkedHashMap<>());
                            response.getExtraData().put("reasoning_content_delta", reasoning);
                            response.getExtraData().put("reasoning_type", "content");
                        }
                        
                        choice.setDelta(delta);
                    }
                    
                    choices.add(choice);
                }
                
                response.setChoices(choices);
            } else {
                log.debug("Kimi流式响应中缺少choices字段");
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("解析Kimi流式响应失败: type={}", e.getClass().getSimpleName());
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

    List<String> extractStreamPayloads(String chunk) {
        if (!StringUtils.hasText(chunk)) return List.of();
        List<String> payloads = new ArrayList<>();
        for (String rawLine : chunk.replace("\r", "").split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith(":") || line.startsWith("event:")
                    || line.startsWith("id:") || line.startsWith("retry:")) continue;
            if (line.startsWith("data:")) line = line.substring(5).trim();
            if (!line.isEmpty() && !"[DONE]".equalsIgnoreCase(line)) payloads.add(line);
        }
        return payloads;
    }

    private String nullableText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return null;
        String value = node.get(field).asText();
        return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }
}
