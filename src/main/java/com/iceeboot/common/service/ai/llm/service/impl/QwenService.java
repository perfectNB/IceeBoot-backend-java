package com.iceeboot.common.service.ai.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.*;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.netty.channel.ChannelOption;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import java.util.*;

/**
 * 通义千问大模型服务实现类
 */
@Slf4j
@Component
public class QwenService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    private static final String DEFAULT_MODEL = "qwen-plus";
    private static final Integer DEFAULT_TIMEOUT = 300;
    private static final Integer DEFAULT_CONNECT_TIMEOUT_MS = 10000;
    private static final Integer DEFAULT_RESPONSE_TIMEOUT = 300;
    private static final Integer DEFAULT_RETRY_MAX = 4;
    private static final Integer DEFAULT_RETRY_BACKOFF_MS = 1000;
    private static final Integer DEFAULT_MAX_CONNECTIONS = 200;
    private static final Integer DEFAULT_PENDING_ACQUIRE_MAX = 1000;
    private static final Integer DEFAULT_PENDING_ACQUIRE_TIMEOUT = 60;
    private static final Integer DEFAULT_MAX_IDLE_TIME = 60;
    private static final Integer DEFAULT_MAX_LIFE_TIME = 300;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public QwenService() {
        ConnectionProvider provider = ConnectionProvider.builder("qwen-connection-provider")
                .maxConnections(DEFAULT_MAX_CONNECTIONS)
                .pendingAcquireMaxCount(DEFAULT_PENDING_ACQUIRE_MAX)
                .pendingAcquireTimeout(Duration.ofSeconds(DEFAULT_PENDING_ACQUIRE_TIMEOUT))
                .maxIdleTime(Duration.ofSeconds(DEFAULT_MAX_IDLE_TIME))
                .maxLifeTime(Duration.ofSeconds(DEFAULT_MAX_LIFE_TIME))
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, DEFAULT_CONNECT_TIMEOUT_MS)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(DEFAULT_RESPONSE_TIMEOUT));
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.QWEN;
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
                            .retryWhen(Retry.backoff(DEFAULT_RETRY_MAX, Duration.ofMillis(DEFAULT_RETRY_BACKOFF_MS))
                                    .filter(this::isRetryable))
                            .doOnError(error -> log.error("通义千问API调用失败: {}", error.getMessage()))
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
                            // .doOnNext(chunk -> log.info("接收到原始数据块: {}", chunk))
                            .filter(chunk -> !chunk.trim().isEmpty() && !"[DONE]".equals(chunk.trim()))
                            // .doOnNext(data -> log.info("过滤后的数据: {}", data))
                            .map(this::parseStreamResponse)
                            .filter(Objects::nonNull)
                            .doOnComplete(() -> log.info("流式响应流完成"))
                            .retryWhen(Retry.backoff(DEFAULT_RETRY_MAX, Duration.ofMillis(DEFAULT_RETRY_BACKOFF_MS))
                                    .filter(this::isRetryable))
                            .doOnError(error -> log.error("通义千问流式API调用失败: {}", error.getMessage(), error))
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
        // 通义千问支持的模型列表 <mcreference link="https://help.aliyun.com/zh/model-studio/use-qwen-by-calling-api" index="1">1</mcreference>
        List<String> models = Arrays.asList(
                "qwen-turbo",
                "qwen-plus",
                "qwen-max",
                "qwen-max-latest",
                "qwen-max-longcontext",
                "qwen2.5-72b-instruct",
                "qwen2.5-32b-instruct",
                "qwen2.5-14b-instruct",
                "qwen2.5-7b-instruct",
                "qwen2.5-3b-instruct",
                "qwen2.5-1.5b-instruct",
                "qwen2.5-0.5b-instruct"
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        // 通义千问模型的最大token限制
        if (model != null) {
            if (model.contains("longcontext")) {
                return 32768; // 长上下文模型
            } else if (model.contains("max")) {
                return 8192; // max系列模型
            }
        }
        return 8192; // 默认限制
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
            if (request.getThinking() != null) {
                requestBody.put("enable_thinking", request.getThinking());
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
            log.error("解析通义千问响应失败: {}", e.getMessage());
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

            // 检查是否有错误
            if (jsonNode.has("error")) {
                JsonNode errorNode = jsonNode.get("error");
                String errorCode = errorNode.has("code") ? errorNode.get("code").asText() : "unknown";
                String errorMessage = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
                response.setError(errorCode + ": " + errorMessage);
                response.setFinished(true);
                log.warn("API返回错误: {} - {}", errorCode, errorMessage);
                return response;
            }

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
            log.error("解析通义千问流式响应失败: {}", e.getMessage());
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

    private boolean isRetryable(Throwable error) {
        return error instanceof IOException
                || error instanceof TimeoutException
                || error instanceof UnknownHostException;
    }
}
