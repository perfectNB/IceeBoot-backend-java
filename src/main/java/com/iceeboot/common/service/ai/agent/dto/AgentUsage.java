/**
 * Agent使用统计DTO类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.ai.agent.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Agent使用统计DTO类
 * @author CodeIcee
 * @date 2025-09-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentUsage {
    
    /**
     * 输入token数量
     */
    private Integer promptTokens;
    
    /**
     * 输出token数量
     */
    private Integer completionTokens;
    
    /**
     * 总token数量
     */
    private Integer totalTokens;
    
    /**
     * 工具调用次数
     */
    private Integer toolCalls;
    
    /**
     * 工作流步骤数
     */
    private Integer workflowSteps;
    
    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;
    
    /**
     * 费用（分）
     */
    private Double cost;
    
    /**
     * 计算总token数
     */
    public void calculateTotalTokens() {
        if (promptTokens != null && completionTokens != null) {
            this.totalTokens = promptTokens + completionTokens;
        }
    }
}