/**
 * Agent消息DTO类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.ai.agent.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Agent消息DTO类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMessage {
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 消息类型：user、assistant、system、tool
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型：text、image、file、tool_call、tool_result
     */
    private String type;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 工具调用信息
     */
    private Map<String, Object> toolCalls;
    
    /**
     * 文件信息
     */
    private Map<String, Object> files;
    
    /**
     * 额外元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 消息状态：pending、completed、failed
     */
    private String status;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 创建用户消息
     * @param content 消息内容
     * @return 用户消息
     */
    public static AgentMessage createUserMessage(String content) {
        return AgentMessage.builder()
                .role("user")
                .content(content)
                .type("text")
                .createdAt(LocalDateTime.now())
                .status("completed")
                .build();
    }
    
    /**
     * 创建助手消息
     * @param content 消息内容
     * @return 助手消息
     */
    public static AgentMessage createAssistantMessage(String content) {
        return AgentMessage.builder()
                .role("assistant")
                .content(content)
                .type("text")
                .createdAt(LocalDateTime.now())
                .status("completed")
                .build();
    }
    
    /**
     * 创建系统消息
     * @param content 消息内容
     * @return 系统消息
     */
    public static AgentMessage createSystemMessage(String content) {
        return AgentMessage.builder()
                .role("system")
                .content(content)
                .type("text")
                .createdAt(LocalDateTime.now())
                .status("completed")
                .build();
    }
}