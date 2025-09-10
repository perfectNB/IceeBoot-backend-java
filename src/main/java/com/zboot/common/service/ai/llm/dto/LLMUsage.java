/**
 * 大模型使用情况统计DTO
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.ai.llm.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 大模型使用情况统计DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class LLMUsage {
    
    /**
     * 提示词token数
     */
    private Integer promptTokens;
    
    /**
     * 完成token数
     */
    private Integer completionTokens;
    
    /**
     * 总token数
     */
    private Integer totalTokens;
    
    /**
     * 提示词缓存命中token数（某些厂商支持）
     */
    private Integer promptCacheHitTokens;
    
    /**
     * 提示词缓存未命中token数（某些厂商支持）
     */
    private Integer promptCacheMissTokens;
}