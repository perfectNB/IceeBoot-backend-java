/**
 * Coze智能体服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.ai.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.service.ai.agent.dto.AgentMessage;
import com.iceeboot.common.service.ai.agent.dto.AgentRequest;
import com.iceeboot.common.service.ai.agent.dto.AgentResponse;
import com.iceeboot.common.service.ai.agent.dto.AgentUsage;
import com.iceeboot.common.service.ai.agent.service.AgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Coze智能体服务实现类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public class CozeAgentService implements AgentService {
    
    private static final String DEFAULT_BASE_URL = "https://api.coze.com";
    private static final String CN_BASE_URL = "https://api.coze.cn";
    private static final String CHAT_ENDPOINT = "/open_api/v2/chat";
    private static final String WORKFLOW_ENDPOINT = "/open_api/v1/workflow/chat";
    private static final String CONVERSATION_ENDPOINT = "/open_api/v1/conversation";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public CozeAgentService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProvider() {
        return "coze";
    }
    
    @Override
    public String getDefaultBaseUrl() {
        return DEFAULT_BASE_URL;
    }
    
    @Override
    public Mono<AgentResponse> chat(AgentRequest request) {
        return buildChatRequest(request, false)
                .flatMap(this::executeChatRequest)
                .map(this::parseChatResponse)
                .onErrorResume(this::handleError);
    }
    
    @Override
    public Flux<AgentResponse> chatStream(AgentRequest request) {
        return buildChatRequest(request, true)
                .flatMapMany(this::executeChatStreamRequest)
                .map(this::parseStreamResponse)
                .onErrorResume(error -> Flux.just(handleErrorSync(error)));
    }
    
    @Override
    public Mono<AgentResponse> runWorkflow(AgentRequest request) {
        return buildWorkflowRequest(request, false)
                .flatMap(this::executeWorkflowRequest)
                .map(this::parseWorkflowResponse)
                .onErrorResume(this::handleError);
    }
    
    @Override
    public Flux<AgentResponse> runWorkflowStream(AgentRequest request) {
        return buildWorkflowRequest(request, true)
                .flatMapMany(this::executeWorkflowStreamRequest)
                .map(this::parseStreamResponse)
                .onErrorResume(error -> Flux.just(handleErrorSync(error)));
    }
    
    @Override
    public Mono<String> createConversation(String userId, String agentId, String apiKey) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("user_id", userId);
        requestBody.put("bot_id", agentId);
        
        String baseUrl = getBaseUrl(null);
        
        return webClient.post()
                .uri(baseUrl + CONVERSATION_ENDPOINT + "/create")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseConversationId)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .doOnError(error -> log.error("创建Coze会话失败: {}", error.getMessage()));
    }
    
    @Override
    public Mono<List<AgentMessage>> getConversationHistory(String conversationId, String apiKey) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.get()
                .uri(baseUrl + CONVERSATION_ENDPOINT + "/message/list?conversation_id=" + conversationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseMessageHistory)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .doOnError(error -> log.error("获取Coze会话历史失败: {}", error.getMessage()));
    }
    
    @Override
    public Mono<Boolean> deleteConversation(String conversationId, String apiKey) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.delete()
                .uri(baseUrl + CONVERSATION_ENDPOINT + "/" + conversationId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .onErrorReturn(false)
                .doOnError(error -> log.error("删除Coze会话失败: {}", error.getMessage()));
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        String url = getBaseUrl(baseUrl);
        
        return webClient.get()
                .uri(url + "/open_api/v1/bots")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedAgents(String apiKey, String baseUrl) {
        String url = getBaseUrl(baseUrl);
        
        return webClient.get()
                .uri(url + "/open_api/v1/bots")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseAgentList)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .onErrorReturn(Collections.emptyList());
    }
    
    @Override
    public Mono<List<String>> getSupportedWorkflows(String apiKey, String baseUrl) {
        String url = getBaseUrl(baseUrl);
        
        return webClient.get()
                .uri(url + "/open_api/v1/workflows")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseWorkflowList)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .onErrorReturn(Collections.emptyList());
    }
    
    private Mono<Map<String, Object>> buildChatRequest(AgentRequest request, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("bot_id", request.getAgentId());
        requestBody.put("user", request.getUserId());
        requestBody.put("query", request.getQuery());
        requestBody.put("stream", stream);
        
        if (request.getConversationId() != null) {
            requestBody.put("conversation_id", request.getConversationId());
        }
        
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            List<Map<String, Object>> messages = request.getMessages().stream()
                    .map(this::convertMessage)
                    .collect(Collectors.toList());
            requestBody.put("additional_messages", messages);
        }
        
        return Mono.just(requestBody);
    }
    
    private Mono<Map<String, Object>> buildWorkflowRequest(AgentRequest request, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("workflow_id", request.getWorkflowId());
        requestBody.put("bot_id", request.getAgentId());
        requestBody.put("user", request.getUserId());
        requestBody.put("stream", stream);
        
        if (request.getConversationId() != null) {
            requestBody.put("conversation_id", request.getConversationId());
        }
        
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            List<Map<String, Object>> messages = request.getMessages().stream()
                    .map(this::convertMessage)
                    .collect(Collectors.toList());
            requestBody.put("additional_messages", messages);
        }
        
        return Mono.just(requestBody);
    }
    
    private Mono<String> executeChatRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.post()
                .uri(baseUrl + CHAT_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getApiKeyFromRequest(requestBody))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private Flux<String> executeChatStreamRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.post()
                .uri(baseUrl + CHAT_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getApiKeyFromRequest(requestBody))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private Mono<String> executeWorkflowRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.post()
                .uri(baseUrl + WORKFLOW_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getApiKeyFromRequest(requestBody))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private Flux<String> executeWorkflowStreamRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.post()
                .uri(baseUrl + WORKFLOW_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + getApiKeyFromRequest(requestBody))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private AgentResponse parseChatResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.has("code") && jsonNode.get("code").asInt() != 0) {
                return AgentResponse.error(
                        String.valueOf(jsonNode.get("code").asInt()),
                        jsonNode.get("msg").asText()
                );
            }
            
            JsonNode data = jsonNode.get("data");
            String content = "";
            String conversationId = data.get("conversation_id").asText();
            String messageId = data.get("id").asText();
            
            if (data.has("messages")) {
                JsonNode messages = data.get("messages");
                if (messages.isArray() && messages.size() > 0) {
                    JsonNode lastMessage = messages.get(messages.size() - 1);
                    content = lastMessage.get("content").asText();
                }
            }
            
            AgentUsage usage = AgentUsage.builder()
                    .totalTokens(data.has("usage") ? data.get("usage").get("token_count").asInt() : 0)
                    .build();
            
            return AgentResponse.builder()
                    .id(messageId)
                    .conversationId(conversationId)
                    .content(content)
                    .status("success")
                    .usage(usage)
                    .createdAt(LocalDateTime.now())
                    .isFinished(true)
                    .rawResponse(response)
                    .build();
            
        } catch (Exception e) {
            log.error("解析Coze响应失败: {}", e.getMessage());
            return AgentResponse.error("PARSE_ERROR", "解析响应失败: " + e.getMessage());
        }
    }
    
    private AgentResponse parseWorkflowResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (jsonNode.has("code") && jsonNode.get("code").asInt() != 0) {
                return AgentResponse.error(
                        String.valueOf(jsonNode.get("code").asInt()),
                        jsonNode.get("msg").asText()
                );
            }
            
            JsonNode data = jsonNode.get("data");
            String content = "";
            String conversationId = data.get("conversation_id").asText();
            
            if (data.has("output")) {
                content = data.get("output").asText();
            }
            
            Map<String, Object> workflowResult = new HashMap<>();
            if (data.has("debug_url")) {
                workflowResult.put("debug_url", data.get("debug_url").asText());
            }
            
            AgentUsage usage = AgentUsage.builder()
                    .totalTokens(data.has("usage") ? data.get("usage").get("token_count").asInt() : 0)
                    .workflowSteps(data.has("step_count") ? data.get("step_count").asInt() : 0)
                    .build();
            
            return AgentResponse.builder()
                    .conversationId(conversationId)
                    .content(content)
                    .status("success")
                    .usage(usage)
                    .workflowResult(workflowResult)
                    .createdAt(LocalDateTime.now())
                    .isFinished(true)
                    .rawResponse(response)
                    .build();
            
        } catch (Exception e) {
            log.error("解析Coze工作流响应失败: {}", e.getMessage());
            return AgentResponse.error("PARSE_ERROR", "解析工作流响应失败: " + e.getMessage());
        }
    }
    
    private AgentResponse parseStreamResponse(String line) {
        try {
            if (line.startsWith("data: ")) {
                String data = line.substring(6);
                if ("[DONE]".equals(data)) {
                    return AgentResponse.builder()
                            .event("done")
                            .status("completed")
                            .isStream(true)
                            .isFinished(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                }
                
                JsonNode jsonNode = objectMapper.readTree(data);
                String event = jsonNode.get("event").asText();
                String content = "";
                
                if ("conversation.message.delta".equals(event)) {
                    content = jsonNode.get("message").get("content").asText();
                }
                
                return AgentResponse.builder()
                        .event(event)
                        .content(content)
                        .status("processing")
                        .isStream(true)
                        .isFinished(false)
                        .createdAt(LocalDateTime.now())
                        .rawResponse(line)
                        .build();
            }
            
            return AgentResponse.stream("unknown", "");
            
        } catch (Exception e) {
            log.error("解析Coze流式响应失败: {}", e.getMessage());
            return AgentResponse.error("PARSE_ERROR", "解析流式响应失败: " + e.getMessage());
        }
    }
    
    private String parseConversationId(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.get("data").get("id").asText();
        } catch (Exception e) {
            log.error("解析会话ID失败: {}", e.getMessage());
            return null;
        }
    }
    
    private List<AgentMessage> parseMessageHistory(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode messages = jsonNode.get("data");
            
            List<AgentMessage> result = new ArrayList<>();
            if (messages.isArray()) {
                for (JsonNode message : messages) {
                    AgentMessage agentMessage = AgentMessage.builder()
                            .messageId(message.get("id").asText())
                            .role(message.get("role").asText())
                            .content(message.get("content").asText())
                            .type(message.get("type").asText())
                            .createdAt(LocalDateTime.now())
                            .build();
                    result.add(agentMessage);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析消息历史失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private List<String> parseAgentList(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode bots = jsonNode.get("data");
            
            List<String> result = new ArrayList<>();
            if (bots.isArray()) {
                for (JsonNode bot : bots) {
                    result.add(bot.get("bot_id").asText());
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析智能体列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private List<String> parseWorkflowList(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode workflows = jsonNode.get("data");
            
            List<String> result = new ArrayList<>();
            if (workflows.isArray()) {
                for (JsonNode workflow : workflows) {
                    result.add(workflow.get("workflow_id").asText());
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析工作流列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private Map<String, Object> convertMessage(AgentMessage message) {
        Map<String, Object> result = new HashMap<>();
        result.put("role", message.getRole());
        result.put("content", message.getContent());
        result.put("content_type", message.getType());
        return result;
    }
    
    private String getBaseUrl(String customBaseUrl) {
        if (customBaseUrl != null && !customBaseUrl.isEmpty()) {
            return customBaseUrl;
        }
        return DEFAULT_BASE_URL;
    }
    
    private String getApiKeyFromRequest(Map<String, Object> requestBody) {
        // 这里应该从请求中获取API密钥，实际实现时需要传递
        return "";
    }
    
    private Mono<AgentResponse> handleError(Throwable error) {
        log.error("Coze服务调用失败: {}", error.getMessage());
        return Mono.just(AgentResponse.error("SERVICE_ERROR", error.getMessage()));
    }
    
    private AgentResponse handleErrorSync(Throwable error) {
        log.error("Coze流式服务调用失败: {}", error.getMessage());
        return AgentResponse.error("SERVICE_ERROR", error.getMessage());
    }
}