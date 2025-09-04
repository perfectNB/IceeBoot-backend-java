/**
 * Agent异常类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.zboot.common.service.agent.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Agent异常类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentException extends RuntimeException {
    
    /**
     * 错误码
     */
    private String errorCode;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 智能体ID
     */
    private String agentId;
    
    /**
     * 工作流ID
     */
    private String workflowId;
    
    /**
     * 会话ID
     */
    private String conversationId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 用户ID
     */
    private String userId;
    
    /**
     * 异常时间戳
     */
    private LocalDateTime timestamp;
    
    public AgentException(String message) {
        super(message);
        this.timestamp = LocalDateTime.now();
    }
    
    public AgentException(String message, Throwable cause) {
        super(message, cause);
        this.timestamp = LocalDateTime.now();
    }
    
    public AgentException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
    
    public AgentException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = LocalDateTime.now();
    }
    
    public AgentException(String errorCode, String message, String provider, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
        this.operation = operation;
        this.timestamp = LocalDateTime.now();
    }
    
    /**
     * 异常工厂类
     */
    public static class Factory {
        
        /**
         * 创建聊天失败异常
         */
        public static AgentException chatFailed(String provider, String message) {
            return new AgentException("CHAT_FAILED", message, provider, "chat");
        }
        
        /**
         * 创建聊天失败异常（带原因）
         */
        public static AgentException chatFailed(String provider, String message, Throwable cause) {
            AgentException exception = new AgentException("CHAT_FAILED", message, cause);
            exception.setProvider(provider);
            exception.setOperation("chat");
            return exception;
        }
        
        /**
         * 创建工作流执行失败异常
         */
        public static AgentException workflowFailed(String provider, String message) {
            return new AgentException("WORKFLOW_FAILED", message, provider, "workflow");
        }
        
        /**
         * 创建工作流执行失败异常（带原因）
         */
        public static AgentException workflowFailed(String provider, String message, Throwable cause) {
            AgentException exception = new AgentException("WORKFLOW_FAILED", message, cause);
            exception.setProvider(provider);
            exception.setOperation("workflow");
            return exception;
        }
        
        /**
         * 创建配置错误异常
         */
        public static AgentException configError(String provider, String message) {
            return new AgentException("CONFIG_ERROR", message, provider, "config");
        }
        
        /**
         * 创建网络错误异常
         */
        public static AgentException networkError(String provider, String message, Throwable cause) {
            AgentException exception = new AgentException("NETWORK_ERROR", message, cause);
            exception.setProvider(provider);
            exception.setOperation("network");
            return exception;
        }
        
        /**
         * 创建超时异常
         */
        public static AgentException timeout(String provider, String operation) {
            return new AgentException("TIMEOUT", "操作超时", provider, operation);
        }
        
        /**
         * 创建无效参数异常
         */
        public static AgentException invalidParameter(String parameter, String message) {
            return new AgentException("INVALID_PARAMETER", "无效参数 " + parameter + ": " + message);
        }
        
        /**
         * 创建认证失败异常
         */
        public static AgentException authenticationFailed(String provider, String message) {
            return new AgentException("AUTHENTICATION_FAILED", message, provider, "auth");
        }
        
        /**
         * 创建限流异常
         */
        public static AgentException rateLimitExceeded(String provider) {
            return new AgentException("RATE_LIMIT_EXCEEDED", "请求频率超限", provider, "rate_limit");
        }
        
        /**
         * 创建不支持的厂商异常
         */
        public static AgentException unsupportedProvider(String provider) {
            return new AgentException("UNSUPPORTED_PROVIDER", "不支持的厂商: " + provider);
        }
        
        /**
         * 创建不支持的操作异常
         */
        public static AgentException unsupportedOperation(String provider, String operation) {
            return new AgentException("UNSUPPORTED_OPERATION", "厂商 " + provider + " 不支持操作: " + operation, provider, operation);
        }
        
        /**
         * 创建解析响应失败异常
         */
        public static AgentException parseResponseFailed(String provider, String message, Throwable cause) {
            AgentException exception = new AgentException("PARSE_RESPONSE_FAILED", message, cause);
            exception.setProvider(provider);
            exception.setOperation("parse");
            return exception;
        }
        
        /**
         * 创建服务不可用异常
         */
        public static AgentException serviceUnavailable(String provider) {
            return new AgentException("SERVICE_UNAVAILABLE", "服务不可用", provider, "service");
        }
        
        /**
         * 创建内部错误异常
         */
        public static AgentException internalError(String message, Throwable cause) {
            return new AgentException("INTERNAL_ERROR", message, cause);
        }
        
        /**
         * 创建会话不存在异常
         */
        public static AgentException conversationNotFound(String provider, String conversationId) {
            AgentException exception = new AgentException("CONVERSATION_NOT_FOUND", "会话不存在: " + conversationId, provider, "conversation");
            exception.setConversationId(conversationId);
            return exception;
        }
        
        /**
         * 创建智能体不存在异常
         */
        public static AgentException agentNotFound(String provider, String agentId) {
            AgentException exception = new AgentException("AGENT_NOT_FOUND", "智能体不存在: " + agentId, provider, "agent");
            exception.setAgentId(agentId);
            return exception;
        }
        
        /**
         * 创建工作流不存在异常
         */
        public static AgentException workflowNotFound(String provider, String workflowId) {
            AgentException exception = new AgentException("WORKFLOW_NOT_FOUND", "工作流不存在: " + workflowId, provider, "workflow");
            exception.setWorkflowId(workflowId);
            return exception;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AgentException{");
        sb.append("errorCode='").append(errorCode).append('\'')
          .append(", provider='").append(provider).append('\'')
          .append(", operation='").append(operation).append('\'')
          .append(", message='").append(getMessage()).append('\'')
          .append(", timestamp=").append(timestamp);
        
        if (agentId != null) {
            sb.append(", agentId='").append(agentId).append('\'');
        }
        if (workflowId != null) {
            sb.append(", workflowId='").append(workflowId).append('\'');
        }
        if (conversationId != null) {
            sb.append(", conversationId='").append(conversationId).append('\'');
        }
        
        sb.append('}');
        return sb.toString();
    }
}