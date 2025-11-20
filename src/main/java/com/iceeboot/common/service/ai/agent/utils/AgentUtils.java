/**
 * Agent工具类
 * 提供Agent相关的通用工具方法
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.ai.agent.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.service.ai.agent.dto.AgentRequest;
import com.iceeboot.common.service.ai.agent.dto.AgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agent工具类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public class AgentUtils {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
    
    /**
     * 验证请求参数
     * @param request 请求对象
     * @return 验证结果
     */
    public static ValidationResult validateRequest(AgentRequest request) {
        if (request == null) {
            return ValidationResult.fail("请求对象不能为空");
        }
        
        if (isBlank(request.getUserId())) {
            return ValidationResult.fail("用户ID不能为空");
        }
        
        if (isBlank(request.getQuery()) && (request.getMessages() == null || request.getMessages().isEmpty())) {
            return ValidationResult.fail("查询内容或消息列表不能同时为空");
        }
        
        if (request.getTimeout() != null && request.getTimeout() <= 0) {
            return ValidationResult.fail("超时时间必须大于0");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证工作流请求参数
     * @param request 请求对象
     * @return 验证结果
     */
    public static ValidationResult validateWorkflowRequest(AgentRequest request) {
        ValidationResult baseResult = validateRequest(request);
        if (!baseResult.isValid()) {
            return baseResult;
        }
        
        if (isBlank(request.getWorkflowId())) {
            return ValidationResult.fail("工作流ID不能为空");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证智能体请求参数
     * @param request 请求对象
     * @return 验证结果
     */
    public static ValidationResult validateAgentRequest(AgentRequest request) {
        ValidationResult baseResult = validateRequest(request);
        if (!baseResult.isValid()) {
            return baseResult;
        }
        
        if (isBlank(request.getAgentId())) {
            return ValidationResult.fail("智能体ID不能为空");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 生成唯一ID
     * @return 唯一ID
     */
    public static String generateId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * 生成请求ID
     * @return 请求ID
     */
    public static String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + generateShortId();
    }
    
    /**
     * 生成短ID
     * @return 短ID
     */
    public static String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 验证UUID格式
     * @param uuid UUID字符串
     * @return 是否有效
     */
    public static boolean isValidUUID(String uuid) {
        return uuid != null && UUID_PATTERN.matcher(uuid).matches();
    }
    
    /**
     * 清理文本内容
     * @param text 原始文本
     * @return 清理后的文本
     */
    public static String cleanText(String text) {
        if (text == null) {
            return null;
        }
        
        return text.trim()
                .replaceAll("\\r\\n|\\r|\\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    /**
     * 截断文本
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    public static String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * 掩码敏感信息
     * @param sensitive 敏感信息
     * @return 掩码后的信息
     */
    public static String maskSensitive(String sensitive) {
        if (isBlank(sensitive)) {
            return sensitive;
        }
        
        if (sensitive.length() <= 8) {
            return "****";
        }
        
        return sensitive.substring(0, 4) + "****" + sensitive.substring(sensitive.length() - 4);
    }
    
    /**
     * 格式化时间戳
     * @param timestamp 时间戳
     * @return 格式化后的时间字符串
     */
    public static String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        
        return timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * 解析JSON字符串
     * @param json JSON字符串
     * @return JsonNode对象
     */
    public static JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("解析JSON失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 对象转JSON字符串
     * @param object 对象
     * @return JSON字符串
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("对象转JSON失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 合并请求参数
     * @param base 基础请求
     * @param override 覆盖请求
     * @return 合并后的请求
     */
    public static AgentRequest mergeRequest(AgentRequest base, AgentRequest override) {
        if (base == null) {
            return override;
        }
        if (override == null) {
            return base;
        }
        
        AgentRequest merged = AgentRequest.builder()
                .agentId(override.getAgentId() != null ? override.getAgentId() : base.getAgentId())
                .workflowId(override.getWorkflowId() != null ? override.getWorkflowId() : base.getWorkflowId())
                .conversationId(override.getConversationId() != null ? override.getConversationId() : base.getConversationId())
                .userId(override.getUserId() != null ? override.getUserId() : base.getUserId())
                .query(override.getQuery() != null ? override.getQuery() : base.getQuery())
                .messages(override.getMessages() != null ? override.getMessages() : base.getMessages())
                .inputs(mergeMap(base.getInputs(), override.getInputs()))
                .stream(override.getStream() != null ? override.getStream() : base.getStream())
                .responseMode(override.getResponseMode() != null ? override.getResponseMode() : base.getResponseMode())
                .apiKey(override.getApiKey() != null ? override.getApiKey() : base.getApiKey())
                .baseUrl(override.getBaseUrl() != null ? override.getBaseUrl() : base.getBaseUrl())
                .timeout(override.getTimeout() != null ? override.getTimeout() : base.getTimeout())
                .extraParams(mergeMap(base.getExtraParams(), override.getExtraParams()))
                .tools(override.getTools() != null ? override.getTools() : base.getTools())
                .files(override.getFiles() != null ? override.getFiles() : base.getFiles())
                .autoSave(override.getAutoSave() != null ? override.getAutoSave() : base.getAutoSave())
                .build();
        
        return merged;
    }
    
    /**
     * 合并Map
     * @param base 基础Map
     * @param override 覆盖Map
     * @return 合并后的Map
     */
    public static Map<String, Object> mergeMap(Map<String, Object> base, Map<String, Object> override) {
        if (base == null && override == null) {
            return null;
        }
        if (base == null) {
            return new HashMap<>(override);
        }
        if (override == null) {
            return new HashMap<>(base);
        }
        
        Map<String, Object> merged = new HashMap<>(base);
        merged.putAll(override);
        return merged;
    }
    
    /**
     * 计算响应大小（字节）
     * @param response 响应对象
     * @return 大小（字节）
     */
    public static long calculateResponseSize(AgentResponse response) {
        if (response == null) {
            return 0;
        }
        
        long size = 0;
        
        if (response.getContent() != null) {
            size += response.getContent().getBytes().length;
        }
        
        if (response.getRawResponse() != null) {
            size += response.getRawResponse().getBytes().length;
        }
        
        return size;
    }
    
    /**
     * 格式化响应大小
     * @param sizeInBytes 字节大小
     * @return 格式化后的大小字符串
     */
    public static String formatSize(long sizeInBytes) {
        if (sizeInBytes < 1024) {
            return sizeInBytes + " B";
        } else if (sizeInBytes < 1024 * 1024) {
            return String.format("%.2f KB", sizeInBytes / 1024.0);
        } else {
            return String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0));
        }
    }
    
    /**
     * 检查字符串是否为空或空白
     * @param str 字符串
     * @return 是否为空或空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    /**
     * 检查字符串是否不为空且不为空白
     * @param str 字符串
     * @return 是否不为空且不为空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}