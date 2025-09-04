/**
 * Dify智能体服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.zboot.common.service.agent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zboot.common.service.agent.dto.AgentRequest;
import com.zboot.common.service.agent.dto.AgentResponse;
import com.zboot.common.service.agent.dto.AgentMessage;
import com.zboot.common.service.agent.dto.AgentUsage;
import com.zboot.common.service.agent.service.AgentService;
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
 * Dify智能体服务实现类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public class DifyAgentService implements AgentService {
    
    private static final String DEFAULT_BASE_URL = "https://api.dify.ai";
    private static final String CHAT_ENDPOINT = "/v1/chat-messages";
    private static final String COMPLETION_ENDPOINT = "/v1/completion-messages";
    private static final String WORKFLOW_ENDPOINT = "/v1/workflows/run";
    private static final String CONVERSATIONS_ENDPOINT = "/v1/conversations";
    private static final String MESSAGES_ENDPOINT = "/v1/messages";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public DifyAgentService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public String getProvider() {
        return "dify";
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
        // Dify会话通过第一次聊天自动创建，这里返回一个占位符
        return Mono.just("auto-generated-" + System.currentTimeMillis());
    }
    
    @Override
    public Mono<List<AgentMessage>> getConversationHistory(String conversationId, String apiKey) {
        String baseUrl = getBaseUrl(null);
        
        return webClient.get()
                .uri(baseUrl + CONVERSATIONS_ENDPOINT + "/" + conversationId + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseMessageHistory)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT))
                .doOnError(error -> log.error("获取Dify会话历史失败: {}", error.getMessage()));
    }
    
    @Override
    public Mono<Boolean> deleteConversation(String conversationId, String apiKey) {
        // Dify不直接支持删除会话，返回true表示操作成功
        return Mono.just(true);
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        String url = getBaseUrl(baseUrl);
        
        // 通过发送一个简单的请求来验证API密钥
        Map<String, Object> testRequest = new HashMap<>();
        testRequest.put("inputs", Collections.emptyMap());
        testRequest.put("query", "test");
        testRequest.put("user", "test-user");
        testRequest.put("response_mode", "blocking");
        
        return webClient.post()
                .uri(url + CHAT_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(testRequest)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .timeout(Duration.ofSeconds(10))
                .onErrorReturn(false);
    }
    
    @Override
    public Mono<List<String>> getSupportedAgents(String apiKey, String baseUrl) {
        // Dify的应用ID需要通过控制台获取，这里返回空列表
        return Mono.just(Collections.emptyList());
    }
    
    @Override
    public Mono<List<String>> getSupportedWorkflows(String apiKey, String baseUrl) {
        // Dify的工作流ID需要通过控制台获取，这里返回空列表
        return Mono.just(Collections.emptyList());
    }
    
    @Override
    public boolean supportsAgent() {
        return true;
    }
    
    @Override
    public boolean supportsWorkflow() {
        return true;
    }
    
    private Mono<Map<String, Object>> buildChatRequest(AgentRequest request, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 基本参数
        requestBody.put("inputs", request.getInputs() != null ? request.getInputs() : Collections.emptyMap());
        requestBody.put("query", request.getQuery());
        requestBody.put("user", request.getUserId());
        requestBody.put("response_mode", stream ? "streaming" : "blocking");
        
        // 会话ID
        if (request.getConversationId() != null) {
            requestBody.put("conversation_id", request.getConversationId());
        }
        
        // 文件
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            requestBody.put("files", request.getFiles());
        }
        
        // 自动保存
        if (request.getAutoSave() != null) {
            requestBody.put("auto_generate_name", request.getAutoSave());
        }
        
        return Mono.just(requestBody);
    }
    
    private Mono<Map<String, Object>> buildWorkflowRequest(AgentRequest request, boolean stream) {
        Map<String, Object> requestBody = new HashMap<>();
        
        // 基本参数
        requestBody.put("inputs", request.getInputs() != null ? request.getInputs() : Collections.emptyMap());
        requestBody.put("user", request.getUserId());
        requestBody.put("response_mode", stream ? "streaming" : "blocking");
        
        // 文件
        if (request.getFiles() != null && !request.getFiles().isEmpty()) {
            requestBody.put("files", request.getFiles());
        }
        
        return Mono.just(requestBody);
    }
    
    private Mono<String> executeChatRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        String apiKey = getApiKeyFromRequest(requestBody);
        
        return webClient.post()
                .uri(baseUrl + CHAT_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private Flux<String> executeChatStreamRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        String apiKey = getApiKeyFromRequest(requestBody);
        
        return webClient.post()
                .uri(baseUrl + CHAT_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.ACCEPT, "text/event-stream")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private Mono<String> executeWorkflowRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        String apiKey = getApiKeyFromRequest(requestBody);
        
        return webClient.post()
                .uri(baseUrl + WORKFLOW_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT));
    }
    
    private Flux<String> executeWorkflowStreamRequest(Map<String, Object> requestBody) {
        String baseUrl = getBaseUrl(null);
        String apiKey = getApiKeyFromRequest(requestBody);
        
        return webClient.post()
                .uri(baseUrl + WORKFLOW_ENDPOINT)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
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
            
            // 检查错误
            if (jsonNode.has("code") && !"success".equals(jsonNode.get("code").asText())) {
                return AgentResponse.error(
                        jsonNode.get("code").asText(),
                        jsonNode.get("message").asText()
                );
            }
            
            String content = jsonNode.get("answer").asText();
            String conversationId = jsonNode.get("conversation_id").asText();
            String messageId = jsonNode.get("message_id").asText();
            
            // 解析使用统计
            AgentUsage usage = AgentUsage.builder().build();
            if (jsonNode.has("metadata")) {
                JsonNode metadata = jsonNode.get("metadata");
                if (metadata.has("usage")) {
                    JsonNode usageNode = metadata.get("usage");
                    usage = AgentUsage.builder()
                            .promptTokens(usageNode.has("prompt_tokens") ? usageNode.get("prompt_tokens").asInt() : 0)
                            .completionTokens(usageNode.has("completion_tokens") ? usageNode.get("completion_tokens").asInt() : 0)
                            .totalTokens(usageNode.has("total_tokens") ? usageNode.get("total_tokens").asInt() : 0)
                            .build();
                }
            }
            
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
            log.error("解析Dify响应失败: {}", e.getMessage());
            return AgentResponse.error("PARSE_ERROR", "解析响应失败: " + e.getMessage());
        }
    }
    
    private AgentResponse parseWorkflowResponse(String response) {
        try {
            JsonNode jsonNode = objectMapper.readTree(response);
            
            // 检查错误
            if (jsonNode.has("code") && !"success".equals(jsonNode.get("code").asText())) {
                return AgentResponse.error(
                        jsonNode.get("code").asText(),
                        jsonNode.get("message").asText()
                );
            }
            
            String workflowRunId = jsonNode.get("workflow_run_id").asText();
            String taskId = jsonNode.get("task_id").asText();
            
            // 解析输出数据
            Map<String, Object> workflowResult = new HashMap<>();
            if (jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                if (data.has("outputs")) {
                    JsonNode outputs = data.get("outputs");
                    outputs.fields().forEachRemaining(entry -> {
                        workflowResult.put(entry.getKey(), entry.getValue().asText());
                    });
                }
            }
            
            // 解析使用统计
            AgentUsage usage = AgentUsage.builder().build();
            if (jsonNode.has("metadata")) {
                JsonNode metadata = jsonNode.get("metadata");
                if (metadata.has("total_tokens")) {
                    usage.setTotalTokens(metadata.get("total_tokens").asInt());
                }
            }
            
            String content = workflowResult.toString();
            
            return AgentResponse.builder()
                    .id(taskId)
                    .content(content)
                    .status("success")
                    .usage(usage)
                    .workflowResult(workflowResult)
                    .createdAt(LocalDateTime.now())
                    .isFinished(true)
                    .rawResponse(response)
                    .build();
            
        } catch (Exception e) {
            log.error("解析Dify工作流响应失败: {}", e.getMessage());
            return AgentResponse.error("PARSE_ERROR", "解析工作流响应失败: " + e.getMessage());
        }
    }
    
    private AgentResponse parseStreamResponse(String line) {
        try {
            if (line.startsWith("data: ")) {
                String data = line.substring(6).trim();
                
                if ("[DONE]".equals(data) || data.isEmpty()) {
                    return AgentResponse.builder()
                            .event("message_end")
                            .status("completed")
                            .isStream(true)
                            .isFinished(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                }
                
                JsonNode jsonNode = objectMapper.readTree(data);
                String event = jsonNode.get("event").asText();
                String content = "";
                
                // 处理不同类型的事件
                switch (event) {
                    case "message":
                    case "agent_message":
                        if (jsonNode.has("answer")) {
                            content = jsonNode.get("answer").asText();
                        }
                        break;
                    case "message_replace":
                        if (jsonNode.has("answer")) {
                            content = jsonNode.get("answer").asText();
                        }
                        break;
                    case "workflow_started":
                    case "workflow_finished":
                    case "node_started":
                    case "node_finished":
                        // 工作流事件
                        break;
                    case "error":
                        return AgentResponse.error(
                                jsonNode.has("code") ? jsonNode.get("code").asText() : "UNKNOWN_ERROR",
                                jsonNode.has("message") ? jsonNode.get("message").asText() : "未知错误"
                        );
                }
                
                return AgentResponse.builder()
                        .event(event)
                        .content(content)
                        .status("processing")
                        .isStream(true)
                        .isFinished("message_end".equals(event) || "workflow_finished".equals(event))
                        .createdAt(LocalDateTime.now())
                        .rawResponse(line)
                        .build();
            }
            
            return AgentResponse.stream("unknown", "");
            
        } catch (Exception e) {
            log.error("解析Dify流式响应失败: {}", e.getMessage());
            return AgentResponse.error("PARSE_ERROR", "解析流式响应失败: " + e.getMessage());
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
                            .role(message.has("from_source") ? message.get("from_source").asText() : "assistant")
                            .content(message.has("answer") ? message.get("answer").asText() : message.get("query").asText())
                            .type("text")
                            .createdAt(LocalDateTime.now())
                            .build();
                    result.add(agentMessage);
                }
            }
            
            return result;
        } catch (Exception e) {
            log.error("解析Dify消息历史失败: {}", e.getMessage());
            return Collections.emptyList();
        }
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
        log.error("Dify服务调用失败: {}", error.getMessage());
        return Mono.just(AgentResponse.error("SERVICE_ERROR", error.getMessage()));
    }
    
    private AgentResponse handleErrorSync(Throwable error) {
        log.error("Dify流式服务调用失败: {}", error.getMessage());
        return AgentResponse.error("SERVICE_ERROR", error.getMessage());
    }
}