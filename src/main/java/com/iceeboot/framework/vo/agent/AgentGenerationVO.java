package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.Map;

/** AgentGenerationVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentGenerationVO")
public class AgentGenerationVO {
    @Schema(description = "采样温度")
    private Double temperature;

    @Schema(description = "最大生成 Token 数")
    private Integer maxTokens;

    @Schema(description = "是否启用思考")
    private Boolean thinking;

    @Schema(description = "思考强度")
    private String reasoningEffort;

    @Schema(description = "思考预算")
    private Integer thinkingBudget;

    @Schema(description = "扩展参数")
    private Map<String, Object> extra;
}
