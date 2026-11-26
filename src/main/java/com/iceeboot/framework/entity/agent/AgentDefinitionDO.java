package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.JdbcType;

/** AgentDefinition 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_definition")
@Schema(description = "智能体定义实体")
public class AgentDefinitionDO extends IceeBootDO {
    @Schema(description = "资源编码")
    @TableField(value = "code", updateStrategy = FieldStrategy.ALWAYS)
    private String code;

    @Schema(description = "资源名称")
    @TableField(value = "name", updateStrategy = FieldStrategy.ALWAYS)
    private String name;

    @Schema(description = "资源说明")
    @TableField(
            value = "description",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String description;

    @Schema(description = "是否启用")
    @TableField(value = "enabled", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean enabled;

    @Schema(description = "头像")
    @TableField(value = "avatar", updateStrategy = FieldStrategy.ALWAYS)
    private String avatar;

    @Schema(description = "系统提示词")
    @TableField(
            value = "prompt",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String prompt;

    @Schema(description = "模型配置 ID")
    @TableField(value = "model_config_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long modelConfigId;

    @Schema(description = "最大执行轮次")
    @TableField(value = "max_rounds", updateStrategy = FieldStrategy.ALWAYS)
    private Integer maxRounds;

    @Schema(description = "是否启用沙箱")
    @TableField(value = "sandbox_enabled", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean sandboxEnabled;

    @Schema(description = "沙箱模板资源 ID")
    @TableField(value = "sandbox_template_id", updateStrategy = FieldStrategy.ALWAYS)
    private Long sandboxTemplateId;

    @Schema(description = "是否启用计划模式")
    @TableField(value = "plan_mode", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean planMode;

    @Schema(description = "输出格式")
    @TableField(value = "output_format", updateStrategy = FieldStrategy.ALWAYS)
    private String outputFormat;

    @Schema(description = "输出 JSON Schema")
    @TableField(
            value = "output_schema_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String outputSchemaJson;

    @Schema(description = "生成参数 JSON")
    @TableField(
            value = "generation_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String generationJson;

    @Schema(description = "绑定 Skill ID JSON")
    @TableField(
            value = "skill_ids_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String skillIdsJson;

    @Schema(description = "绑定 Tool ID JSON")
    @TableField(
            value = "tool_ids_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String toolIdsJson;

    @Schema(description = "绑定 MCP ID JSON")
    @TableField(
            value = "mcp_ids_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String mcpIdsJson;

    @Schema(description = "绑定知识库 ID JSON")
    @TableField(
            value = "knowledge_ids_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String knowledgeIdsJson;

    @Schema(description = "子智能体 ID JSON")
    @TableField(
            value = "child_agent_ids_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String childAgentIdsJson;

    @Schema(description = "加密环境变量 JSON")
    @TableField(
            value = "environment_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String environmentJson;

    @Schema(description = "当前发布版本 ID")
    @TableField(value = "active_release_id", updateStrategy = FieldStrategy.NEVER)
    private String activeReleaseId;

    @TableField(exist = false)
    private Integer activeVersion;

    @TableField(exist = false)
    private Long callCount;

    @TableField(exist = false)
    private Long totalInputTokens;

    @TableField(exist = false)
    private Long totalOutputTokens;

    @TableField(exist = false)
    private Long totalCachedTokens;
}
