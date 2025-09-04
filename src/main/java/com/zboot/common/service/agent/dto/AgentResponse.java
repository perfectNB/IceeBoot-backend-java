/**
 * Agent响应DTO类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.zboot.common.service.agent.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Agent响应DTO类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {
    
    /**
     * 响应ID
     */
    private String id;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 响应消息
     */
    private AgentMessage message;
    
    /**
     * 消息列表
     */
    private List<AgentMessage> messages;
    
    /**
     * 事件类型：message、error、done、tool_call等
     */
    private String event;
    
    /**
     * 响应状态：success、error、processing、completed
     */
    private String status;
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 完成时间
     */
    private LocalDateTime finishedAt;
    
    /**
     * 使用统计
     */
    private AgentUsage usage;
    
    /**
     * 工具调用结果
     */
    private List<Map<String, Object>> toolCalls;
    
    /**
     * 工作流执行结果
     */
    private Map<String, Object> workflowResult;
    
    /**
     * 额外数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 原始响应数据
     */
    private String rawResponse;
    
    /**
     * 是否流式响应
     */
    private Boolean isStream;
    
    /**
     * 流式响应是否结束
     */
    private Boolean isFinished;
    
    /**
     * 创建成功响应
     * @param content 响应内容
     * @return 成功响应
     */
    public static AgentResponse success(String content) {
        return AgentResponse.builder()
                .content(content)
                .status("success")
                .createdAt(LocalDateTime.now())
                .isFinished(true)
                .build();
    }
    
    /**
     * 创建错误响应
     * @param errorCode 错误码
     * @param errorMessage 错误信息
     * @return 错误响应
     */
    public static AgentResponse error(String errorCode, String errorMessage) {
        return AgentResponse.builder()
                .status("error")
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .isFinished(true)
                .build();
    }
    
    /**
     * 创建流式响应
     * @param event 事件类型
     * @param content 内容
     * @return 流式响应
     */
    public static AgentResponse stream(String event, String content) {
        return AgentResponse.builder()
                .event(event)
                .content(content)
                .status("processing")
                .isStream(true)
                .isFinished(false)
                .createdAt(LocalDateTime.now())
                .build();
    }
}