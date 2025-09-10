/**
 * Agent请求DTO类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.zboot.common.service.ai.agent.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Agent请求DTO类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {
    
    /**
     * 智能体ID
     */
    private String agentId;
    
    /**
     * 工作流ID（可选）
     */
    private String workflowId;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 用户输入消息
     */
    private String query;
    
    /**
     * 消息列表（用于多轮对话）
     */
    private List<AgentMessage> messages;
    
    /**
     * 输入参数
     */
    private Map<String, Object> inputs;
    
    /**
     * 是否流式输出
     */
    private Boolean stream;
    
    /**
     * 响应模式：blocking（阻塞）、streaming（流式）
     */
    private String responseMode;
    
    /**
     * API密钥
     */
    private String apiKey;
    
    /**
     * 基础URL
     */
    private String baseUrl;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout;
    
    /**
     * 额外参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 工具列表
     */
    private List<String> tools;
    
    /**
     * 文件列表
     */
    private List<String> files;
    
    /**
     * 自动保存会话
     */
    private Boolean autoSave;
}