package com.iceeboot.common.service.ai.llm.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * SSE工具类
 */
@Slf4j
public class SSEUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 将LLMResponse流转换为SSE事件流
     * @param responseFlux LLM响应流
     * @return SSE事件流
     */
    public static Flux<ServerSentEvent<String>> toSSEFlux(Flux<LLMResponse> responseFlux) {
        return responseFlux
                .map(SSEUtils::convertToSSEEvent)
                .concatWith(Flux.just(createDoneEvent()))
                .doOnError(error -> log.error("SSE流处理错误: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just(createErrorEvent(error.getMessage())));
    }
    
    /**
     * 将LLMResponse流转换为JSON字符串流（用于前端直接消费）
     * @param responseFlux LLM响应流
     * @return JSON字符串流
     */
    public static Flux<String> toJsonFlux(Flux<LLMResponse> responseFlux) {
        return responseFlux
                .map(SSEUtils::convertToJsonString)
                .concatWith(Flux.just(createDoneJson()))
                .doOnError(error -> log.error("JSON流处理错误: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just(createErrorJson(error.getMessage())));
    }
    
    /**
     * 创建带心跳的SSE流
     * @param responseFlux LLM响应流
     * @param heartbeatInterval 心跳间隔（秒）
     * @return 带心跳的SSE事件流
     */
    public static Flux<ServerSentEvent<String>> toSSEFluxWithHeartbeat(
            Flux<LLMResponse> responseFlux, 
            int heartbeatInterval) {
        
        // 心跳流
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(heartbeatInterval))
                .map(tick -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("ping")
                        .build());
        
        // 数据流
        Flux<ServerSentEvent<String>> dataStream = toSSEFlux(responseFlux);
        
        // 合并流，数据流优先
        return Flux.merge(dataStream, heartbeat)
                .takeUntilOther(dataStream.filter(event -> "done".equals(event.event()) || "error".equals(event.event())))
                .distinct(); // 去重，避免重复的心跳
    }
    
    /**
     * 创建Vue3+TS前端可直接使用的EventSource格式流
     * @param responseFlux LLM响应流
     * @return EventSource格式的字符串流
     */
    public static Flux<String> toEventSourceFlux(Flux<LLMResponse> responseFlux) {
        return responseFlux
                .map(response -> {
                    try {
                        String jsonData = objectMapper.writeValueAsString(response);
                        return "data: " + jsonData + "\n\n";
                    } catch (Exception e) {
                        log.error("转换EventSource格式失败: {}", e.getMessage());
                        return "data: {\"error\":\"数据转换失败\"}\n\n";
                    }
                })
                .concatWith(Flux.just("data: [DONE]\n\n"))
                .doOnError(error -> log.error("EventSource流处理错误: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just("data: {\"error\":\"" + error.getMessage() + "\"}\n\n"));
    }
    
    /**
     * 创建支持推理内容分离的SSE流
     * 将推理内容和最终答案分别作为不同的事件发送
     * @param responseFlux LLM响应流
     * @return 分离推理内容的SSE事件流
     */
    public static Flux<ServerSentEvent<String>> toReasoningSSEFlux(Flux<LLMResponse> responseFlux) {
        return responseFlux
                .flatMap(response -> {
                    Flux<ServerSentEvent<String>> events = Flux.empty();
                    
                    // 处理推理内容
                    if (response.getExtraData() != null) {
                        String reasoningContent = (String) response.getExtraData().get("reasoning_content_delta");
                        if (reasoningContent != null && !reasoningContent.trim().isEmpty()) {
                            ServerSentEvent<String> reasoningEvent = ServerSentEvent.<String>builder()
                                    .event("reasoning")
                                    .data(reasoningContent)
                                    .build();
                            events = events.concatWith(Flux.just(reasoningEvent));
                        }
                    }
                    
                    // 处理正常内容
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = extractContentFromResponse(response);
                        if (content != null && !content.trim().isEmpty()) {
                            ServerSentEvent<String> contentEvent = ServerSentEvent.<String>builder()
                                    .event("content")
                                    .data(content)
                                    .build();
                            events = events.concatWith(Flux.just(contentEvent));
                        }
                    }
                    
                    // 处理完成事件
                    if (Boolean.TRUE.equals(response.getFinished())) {
                        ServerSentEvent<String> finishEvent = ServerSentEvent.<String>builder()
                                .event("finish")
                                .data("completed")
                                .build();
                        events = events.concatWith(Flux.just(finishEvent));
                    }
                    
                    return events;
                })
                .concatWith(Flux.just(createDoneEvent()))
                .doOnError(error -> log.error("推理SSE流处理错误: {}", error.getMessage()))
                .onErrorResume(error -> Flux.just(createErrorEvent(error.getMessage())));
    }
    
    /**
     * 获取合适的MediaType
     * @return SSE的MediaType
     */
    public static MediaType getSSEMediaType() {
        return MediaType.TEXT_EVENT_STREAM;
    }
    
    /**
     * 获取JSON流的MediaType
     * @return JSON流的MediaType
     */
    public static MediaType getJsonStreamMediaType() {
        return MediaType.APPLICATION_NDJSON;
    }
    
    /**
     * 转换LLMResponse为SSE事件
     */
    private static ServerSentEvent<String> convertToSSEEvent(LLMResponse response) {
        try {
            String jsonData = objectMapper.writeValueAsString(response);
            return ServerSentEvent.<String>builder()
                    .event("message")
                    .data(jsonData)
                    .build();
        } catch (Exception e) {
            log.error("转换SSE事件失败: {}", e.getMessage());
            return createErrorEvent("数据转换失败");
        }
    }
    
    /**
     * 转换LLMResponse为JSON字符串
     */
    private static String convertToJsonString(LLMResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("转换JSON字符串失败: {}", e.getMessage());
            return "{\"error\":\"数据转换失败\"}";
        }
    }
    
    /**
     * 从响应中提取内容
     */
    private static String extractContentFromResponse(LLMResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            var choice = response.getChoices().get(0);
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                return choice.getDelta().getContent();
            }
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
        }
        return null;
    }
    
    /**
     * 创建完成事件
     */
    private static ServerSentEvent<String> createDoneEvent() {
        return ServerSentEvent.<String>builder()
                .event("done")
                .data("completed")
                .build();
    }
    
    /**
     * 创建错误事件
     */
    private static ServerSentEvent<String> createErrorEvent(String errorMessage) {
        return ServerSentEvent.<String>builder()
                .event("error")
                .data("{\"error\":\"" + errorMessage + "\"}")
                .build();
    }
    
    /**
     * 创建完成JSON
     */
    private static String createDoneJson() {
        return "{\"event\":\"done\",\"data\":\"completed\"}";
    }
    
    /**
     * 创建错误JSON
     */
    private static String createErrorJson(String errorMessage) {
        return "{\"event\":\"error\",\"data\":{\"error\":\"" + errorMessage + "\"}}"; 
    }
}