package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** 智能体定义请求参数。 */
@Data
@Schema(description = "智能体定义请求参数")
public class AgentDefinitionDTO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "编码")
    @NotBlank
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}")
    private String code;

    @Schema(description = "名称")
    @NotBlank
    @Size(max = 120)
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "头像")
    private String avatar;

    @Schema(description = "系统提示词")
    @NotBlank
    @Size(max = 100000)
    private String prompt;

    @Schema(description = "模型连接 ID")
    @NotNull
    private Long modelConfigId;

    @Schema(description = "最大执行轮次")
    @Min(1)
    @Max(100)
    private int maxRounds;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "是否启用沙箱")
    private boolean sandboxEnabled;

    @Schema(description = "沙箱模板资源 ID")
    private Long sandboxTemplateId;

    @Schema(description = "是否启用计划模式")
    private boolean planMode;

    @Schema(description = "输出格式")
    @NotNull
    private OutputFormat outputFormat;

    @Schema(description = "输出 JSON Schema")
    private Map<String, Object> outputSchema;

    @Schema(description = "生成参数")
    @Valid
    private AgentGenerationDTO generation;

    @Schema(description = "绑定技能 ID")
    private List<Long> skillIds;

    @Schema(description = "绑定工具 ID")
    private List<Long> toolIds;

    @Schema(description = "绑定 MCP ID")
    private List<Long> mcpIds;

    @Schema(description = "绑定知识库 ID")
    private List<Long> knowledgeIds;

    @Schema(description = "绑定子智能体 ID")
    private List<Long> childAgentIds;

    @Schema(description = "沙箱环境变量，响应中返回脱敏值")
    private Map<String, String> environment;
}
