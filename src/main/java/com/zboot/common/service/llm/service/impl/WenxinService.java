/**
 * 文心一言大模型服务实现类
 * 百度文心一言使用OAuth2.0认证方式，支持流式和非流式输出
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.llm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zboot.common.constant.SystemConstants;
import com.zboot.common.service.llm.dto.*;
import com.zboot.common.service.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文心一言大模型服务实现类
 * 注意：文心一言使用OAuth2.0认证，需要先获取access_token
 */
@Slf4j
public class WenxinService implements LLMService {
    
    private static final String DEFAULT_BASE_URL = "https://aip.baidubce.com";
    private static final String DEFAULT_MODEL = "ernie-bot-turbo";
    private static final String TOKEN_ENDPOINT = "/oauth/2.0/token";
    private static final String CHAT_ENDPOINT = "/rpc/2.0/ai/v1/chat/completions";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    // 缓存access_token，避免频繁获取
    private static final Map<String, TokenInfo> tokenCache = new ConcurrentHashMap<>();
    
    public WenxinService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.WENXIN;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        return getValidAccessToken(request.getApiKey(), request.getSecretKey())
                .flatMap(accessToken -> buildRequestBody(request, false)
                        .flatMap(requestBody -> {
                            String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                            String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
                            String endpoint = getModelEndpoint(model);
                            
                            return webClient.post()
                                    .uri(baseUrl + endpoint + "?access_token=" + accessToken)
                                    .header("Content-Type", "application/json")
                                    .bodyValue(requestBody)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .doOnNext(responseBody -> log.info("文心一言API原始响应: {}", responseBody))
                            .map(this::parseResponse)
                            .doOnError(error -> log.error("文心一言API调用失败: {}", error.getMessage()))
                            .onErrorReturn(createErrorResponse("API调用失败"));
                        }));
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        return getValidAccessToken(request.getApiKey(), request.getSecretKey())
                .flatMapMany(accessToken -> buildRequestBody(request, true)
                        .flatMapMany(requestBody -> {
                            String baseUrl = request.getBaseUrl() != null ? request.getBaseUrl() : DEFAULT_BASE_URL;
                            String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
                            String endpoint = getModelEndpoint(model);
                            
                            return webClient.post()
                                    .uri(baseUrl + endpoint + "?access_token=" + accessToken)
                                    .header("Content-Type", "application/json")
                                    .header("Accept", "text/event-stream")
                                    .bodyValue(requestBody)
                                    .retrieve()
                                    .bodyToFlux(String.class)
                                    .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .doOnNext(chunk -> log.info("接收到原始数据块: {}", chunk))
                            .filter(chunk -> !chunk.trim().isEmpty() && !"[DONE]".equals(chunk.trim()))
                            .doOnNext(data -> log.info("处理JSON数据: {}", data))
                            .map(this::parseStreamResponse)
                            .filter(Objects::nonNull)
                            .doOnError(error -> log.error("文心一言流式API调用失败: {}", error.getMessage()))
                            .onErrorReturn(createErrorResponse("流式API调用失败"));
                        }));
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        // 文心一言需要同时验证API Key和Secret Key
        // 这里假设apiKey格式为 "apiKey:secretKey"
        String[] keys = parseApiKeyAndSecret(apiKey);
        if (keys.length != 2) {
            return Mono.just(false);
        }
        
        return getValidAccessToken(keys[0], keys[1])
                .map(token -> token != null && !token.trim().isEmpty())
                .onErrorReturn(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "ernie-bot",
                "ernie-bot-turbo",
                "ernie-bot-4",
                "ernie-3.5",
                "ernie-speed",
                "ernie-lite",
                "ernie-tiny-8k",
                "ernie-char-8k",
                "ernie-text-embedding"
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
            if (model.contains("ernie-bot-4")) {
                return 8192;
            } else if (model.contains("8k")) {
                return 8192;
            } else if (model.contains("tiny")) {
                return 1024;
            }
        }
        return 4096;
    }
    
    /**
     * 获取有效的访问令牌
     */
    private Mono<String> getValidAccessToken(String apiKey, String secretKey) {
        String cacheKey = apiKey + ":" + secretKey;
        TokenInfo tokenInfo = tokenCache.get(cacheKey);
        
        // 检查缓存的token是否有效
        if (tokenInfo != null && !tokenInfo.isExpired()) {
            return Mono.just(tokenInfo.getAccessToken());
        }
        
        // 获取新的access_token
        return getAccessToken(apiKey, secretKey)
                .doOnNext(token -> {
                    // 缓存token，设置过期时间为25天（百度token有效期30天）
                    long expiresAt = System.currentTimeMillis() + 25 * 24 * 60 * 60 * 1000L;
                    tokenCache.put(cacheKey, new TokenInfo(token, expiresAt));
                });
    }
    
    /**
     * 获取访问令牌
     * 文心一言特有的OAuth2.0认证流程
     */
    private Mono<String> getAccessToken(String apiKey, String secretKey) {
        String tokenUrl = DEFAULT_BASE_URL + TOKEN_ENDPOINT + 
                "?grant_type=client_credentials&client_id=" + apiKey + 
                "&client_secret=" + secretKey;
        
        return webClient.post()
                .uri(tokenUrl)
                .header("Content-Type", "application/json")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(this::parseAccessToken)
                .doOnError(error -> log.error("获取文心一言访问令牌失败: {}", error.getMessage()))
                .onErrorReturn("");
    }
    
    /**
     * 解析访问令牌响应
     */
    private String parseAccessToken(String responseBody) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseBody);
            if (jsonNode.has("access_token")) {
                return jsonNode.get("access_token").asText();
            } else if (jsonNode.has("error")) {
                log.error("获取访问令牌失败: {}", jsonNode.get("error_description").asText());
            }
        } catch (Exception e) {
            log.error("解析访问令牌响应失败: {}", e.getMessage());
        }
        return "";
    }
    
    /**
     * 根据模型获取对应的API端点
     * 文心一言使用百度自己的API格式，不是OpenAI兼容格式
     */
    private String getModelEndpoint(String model) {
        if (model == null) {
            return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/eb-instant";
        }
        
        switch (model) {
            case "ernie-bot":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions";
            case "ernie-bot-turbo":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/eb-instant";
            case "ernie-bot-4":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions_pro";
            case "ernie-3.5":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-3.5-4k-0205";
            case "ernie-speed":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie_speed";
            case "ernie-lite":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-lite-8k";
            case "ernie-tiny-8k":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-tiny-8k";
            case "ernie-char-8k":
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/ernie-char-8k";
            default:
                return "/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/eb-instant";
        }
    }
    
    /**
     * 解析API Key和Secret Key
     */
    private String[] parseApiKeyAndSecret(String apiKey) {
        if (apiKey != null && apiKey.contains(":")) {
            return apiKey.split(":", 2);
        }
        return new String[]{apiKey, ""};
    }
    
    /**
     * 构建请求体
     */
    private Mono<Map<String, Object>> buildRequestBody(LLMRequest request, boolean stream) {
        return Mono.fromCallable(() -> {
            Map<String, Object> requestBody = new HashMap<>();
            
            requestBody.put("stream", stream);
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 文心一言不支持system角色，需要将系统提示词合并到第一条用户消息中
            String systemPrompt = request.getSystemPrompt();
            
            // 添加历史消息
            if (request.getMessages() != null && !request.getMessages().isEmpty()) {
                boolean firstUserMessage = true;
                for (LLMMessage msg : request.getMessages()) {
                    // 跳过system角色的消息，文心一言不支持
                    if ("system".equals(msg.getRole())) {
                        continue;
                    }
                    
                    Map<String, String> message = new HashMap<>();
                    message.put("role", msg.getRole());
                    
                    // 如果是第一条用户消息且有系统提示词，则合并
                    if ("user".equals(msg.getRole()) && firstUserMessage && systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                        message.put("content", systemPrompt + "\n\n" + msg.getContent());
                        firstUserMessage = false;
                    } else {
                        message.put("content", msg.getContent());
                    }
                    
                    messages.add(message);
                }
            } else if (request.getUserMessage() != null && !request.getUserMessage().trim().isEmpty()) {
                // 添加用户消息，如果有系统提示词则合并
                Map<String, String> userMessage = new HashMap<>();
                userMessage.put("role", "user");
                
                if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                    userMessage.put("content", systemPrompt + "\n\n" + request.getUserMessage());
                } else {
                    userMessage.put("content", request.getUserMessage());
                }
                
                messages.add(userMessage);
            }
            
            requestBody.put("messages", messages);
            
            // 设置其他参数
            if (request.getTemperature() != null) {
                requestBody.put("temperature", request.getTemperature());
            }
            
            if (request.getMaxTokens() != null) {
                requestBody.put("max_output_tokens", request.getMaxTokens());
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
            if (jsonNode.has("created")) {
                response.setCreated(jsonNode.get("created").asLong());
            }
            
            // 解析结果内容
            if (jsonNode.has("result")) {
                String content = jsonNode.get("result").asText();
                
                LLMChoice choice = new LLMChoice();
                choice.setIndex(0);
                choice.setFinishReason("stop");
                
                LLMMessage message = new LLMMessage();
                message.setRole("assistant");
                message.setContent(content);
                choice.setMessage(message);
                
                response.setChoices(Arrays.asList(choice));
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
            log.error("解析文心一言响应失败: {}", e.getMessage());
            return createErrorResponse("响应解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 解析流式响应
     */
    private LLMResponse parseStreamResponse(String data) {
        try {
            // 处理SSE格式数据
            if (data.startsWith("data: ")) {
                data = data.substring(6).trim();
            }
            
            JsonNode jsonNode = objectMapper.readTree(data);
            
            LLMResponse response = new LLMResponse()
                    .setProvider(getProvider())
                    .setFinished(false);
            
            // 解析基本信息
            if (jsonNode.has("id")) {
                response.setId(jsonNode.get("id").asText());
            }
            if (jsonNode.has("created")) {
                response.setCreated(jsonNode.get("created").asLong());
            }
            
            // 解析结果内容
            if (jsonNode.has("result")) {
                String content = jsonNode.get("result").asText();
                
                LLMChoice choice = new LLMChoice();
                choice.setIndex(0);
                
                // 检查是否完成
                if (jsonNode.has("is_end") && jsonNode.get("is_end").asBoolean()) {
                    choice.setFinishReason("stop");
                    response.setFinished(true);
                }
                
                LLMMessage delta = new LLMMessage();
                delta.setRole("assistant");
                delta.setContent(content);
                choice.setDelta(delta);
                
                response.setChoices(Arrays.asList(choice));
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("解析文心一言流式响应失败: {}", e.getMessage());
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
    
    /**
     * Token信息缓存类
     */
    private static class TokenInfo {
        private final String accessToken;
        private final long expiresAt;
        
        public TokenInfo(String accessToken, long expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }
        
        public String getAccessToken() {
            return accessToken;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}