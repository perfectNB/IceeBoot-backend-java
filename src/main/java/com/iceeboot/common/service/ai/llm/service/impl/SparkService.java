package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.*;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;

/**
 * 讯飞星火大模型服务实现类
 * 注意：讯飞星火主要使用WebSocket协议，此实现为HTTP兼容版本
 */
@Slf4j
@Component
public class SparkService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://spark-api-open.xf-yun.com/v1";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    private static final String DEFAULT_MODEL = "generalv3.5";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public SparkService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.SPARK;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        // 讯飞星火主要使用WebSocket协议，HTTP协议支持有限
        log.warn("讯飞星火主要使用WebSocket协议，当前HTTP实现可能无法正常工作");
        
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
                            .doOnError(error -> log.error("讯飞星火API调用失败: {}，建议使用WebSocket协议实现", error.getMessage()))
                            .onErrorReturn(createErrorResponse("API调用失败，讯飞星火主要使用WebSocket协议，建议实现WebSocket版本"));
                });
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        // 讯飞星火主要使用WebSocket协议，HTTP协议支持有限
        log.warn("讯飞星火主要使用WebSocket协议，当前HTTP流式实现可能无法正常工作");
        
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
                            .doOnError(error -> log.error("讯飞星火流式API调用失败: {}，建议使用WebSocket协议实现", error.getMessage()))
                            .onErrorReturn(createErrorResponse("流式API调用失败，讯飞星火主要使用WebSocket协议，建议实现WebSocket版本"));
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
        // 讯飞星火支持的模型列表 <mcreference link="https://www.xfyun.cn/doc/spark/Web.html" index="1">1</mcreference>
        List<String> models = Arrays.asList(
                "generalv3.5",
                "generalv3",
                "generalv2",
                "pro-128k",
                "max-32k",
                "4.0Ultra"
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
            } else if (model.contains("4.0Ultra")) {
                return 8192;
            }
        }
        return 8192;
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
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            
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
            log.error("解析讯飞星火响应失败: {}", e.getMessage());
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
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("解析讯飞星火流式响应失败: {}", e.getMessage());
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