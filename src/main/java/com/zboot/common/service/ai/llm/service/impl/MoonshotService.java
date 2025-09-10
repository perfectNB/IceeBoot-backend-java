/**
 * Kimi（月之暗面）大模型服务实现类
 * 支持流式输出、非流式输出功能
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.ai.llm.dto.*;
import com.zboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * Kimi（月之暗面）大模型服务实现类
 */
@Slf4j
public class MoonshotService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://api.moonshot.cn/v1";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    private static final String DEFAULT_MODEL = "moonshot-v1-8k";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public MoonshotService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
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
                    
                    return webClient.post()
                            .uri(baseUrl + CHAT_ENDPOINT)
                            .header("Authorization", "Bearer " + request.getApiKey())
                            .header("Content-Type", "application/json")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .map(this::parseResponse)
                            .doOnError(error -> log.error("Kimi API调用失败: {}", error.getMessage()))
                            .onErrorReturn(createErrorResponse("API调用失败"));
                });
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        return buildRequestBody(request, true)
                .flatMapMany(requestBody -> {
                    String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                    
                    return webClient.post()
                            .uri(baseUrl + CHAT_ENDPOINT)
                            .header("Authorization", "Bearer " + request.getApiKey())
                            .header("Content-Type", "application/json")
                            .header("Accept", "text/event-stream")
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToFlux(String.class)
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .doOnNext(chunk -> log.debug("接收到原始数据块: {}", chunk))
                            .filter(chunk -> !chunk.trim().isEmpty() && !"[DONE]".equals(chunk.trim()))
                            .doOnNext(data -> log.debug("处理JSON数据: {}", data))
                            .map(this::parseStreamResponse)
                            .filter(Objects::nonNull)
                            .doOnError(error -> log.error("Kimi流式API调用失败: {}", error.getMessage()))
                            .onErrorReturn(createErrorResponse("流式API调用失败"));
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
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示词
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", request.getSystemPrompt());
                messages.add(systemMessage);
            }
            
            // 添加历史消息
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                for (LLMMessage msg : request.getMessages()) {
                    Map<String, String> message = new HashMap<>();
                    message.put("role", msg.getRole());
                    message.put("content", msg.getContent());
                    messages.add(message);
                }
            } else if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
                // 添加用户消息
                Map<String, String> userMessage = new HashMap<>();
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
            
            return requestBody;
        });
    }
    
    /**
     * 解析非流式响应
     */
    private LLMResponse parseResponse(String responseBody) {
        try {
            log.debug("Kimi原始响应: {}", responseBody);
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
                log.error("Kimi API返回错误: {}", errorMessage);
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
                        
                        choice.setMessage(message);
                    }
                    
                    choices.add(choice);
                }
                
                response.setChoices(choices);
            } else {
                log.warn("Kimi响应中缺少choices字段，响应结构: {}", jsonNode.toString());
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
            log.error("解析Kimi响应失败，原始响应: {}, 错误: {}", responseBody, e.getMessage(), e);
            return createErrorResponse("响应解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析流式响应
     */
    private LLMResponse parseStreamResponse(String data) {
        try {
            log.debug("Kimi流式响应片段: {}", data);
            JsonNode jsonNode = objectMapper.readTree(data);
            
            // 检查是否有错误信息
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorMessage = "流式API调用失败";
                if (errorNode.has("message")) {
                    errorMessage = errorNode.get("message").asText();
                }
                log.error("Kimi流式API返回错误: {}", errorMessage);
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
                        if ("stop".equals(finishReason)) {
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
                        
                        choice.setDelta(delta);
                    }
                    
                    choices.add(choice);
                }
                
                response.setChoices(choices);
            } else {
                log.debug("Kimi流式响应中缺少choices字段，可能是心跳包: {}", data);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("解析Kimi流式响应失败，原始数据: {}, 错误: {}", data, e.getMessage(), e);
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
}