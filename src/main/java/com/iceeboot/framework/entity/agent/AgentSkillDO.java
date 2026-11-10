package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.JdbcType;

/** AgentSkill 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_skill")
@Schema(description = "技能实体")
public class AgentSkillDO extends IceeBootDO {
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

    @Schema(description = "Skill 正文")
    @TableField(
            value = "content",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String content;

    @Schema(description = "Skill 资源 JSON")
    @TableField(
            value = "resources_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String resourcesJson;

    @Schema(description = "关联工具 ID JSON")
    @TableField(
            value = "tool_ids_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String toolIdsJson;
}
