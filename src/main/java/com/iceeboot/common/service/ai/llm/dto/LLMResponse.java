/**
 * 大模型响应DTO
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.ai.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * 大模型响应DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class LLMResponse {
    
    /**
     * 响应ID
     */
    private String id;
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 创建时间戳
     */
    private Long created;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 选择列表
     */
    private List<LLMChoice> choices;
    
    /**
     * 使用情况统计
     */
    private LLMUsage usage;
    
    /**
     * 是否完成
     */
    private Boolean finished;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;
}