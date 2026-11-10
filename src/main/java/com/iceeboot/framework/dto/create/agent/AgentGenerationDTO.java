package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.Map;

/** AgentGenerationDTO请求参数。 */
@Data
@Schema(description = "AgentGenerationDTO")
public class AgentGenerationDTO {
    @Schema(description = "采样温度")
    @DecimalMin("0")
    @DecimalMax("2")
    private Double temperature;

    @Schema(description = "最大生成 Token 数")
    @Min(1)
    @Max(1048576)
    private Integer maxTokens;

    @Schema(description = "是否启用思考")
    private Boolean thinking;

    @Schema(description = "思考强度")
    private String reasoningEffort;

    @Schema(description = "思考预算")
    @Min(1024)
    private Integer thinkingBudget;

    @Schema(description = "扩展参数")
    private Map<String, Object> extra;
}
