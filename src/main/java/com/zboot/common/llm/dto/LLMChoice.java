/**
 * 大模型选择DTO
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 大模型选择DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class LLMChoice {
    
    /**
     * 选择索引
     */
    private Integer index;
    
    /**
     * 消息内容
     */
    private LLMMessage message;
    
    /**
     * 增量内容（流式输出时使用）
     */
    private LLMMessage delta;
    
    /**
     * 完成原因：stop、length、content_filter等
     */
    private String finishReason;
    
    /**
     * 日志概率信息
     */
    private Object logprobs;
}