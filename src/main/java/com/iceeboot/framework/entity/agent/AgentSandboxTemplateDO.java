package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.JdbcType;

/** AgentSandboxTemplate 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_sandbox_template")
@Schema(description = "沙箱模板实体")
public class AgentSandboxTemplateDO extends IceeBootDO {
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

    @Schema(description = "E2B 模板标识")
    @TableField(value = "template_id", updateStrategy = FieldStrategy.ALWAYS)
    private String templateId;

    @Schema(description = "沙箱超时秒数")
    @TableField(value = "timeout_seconds", updateStrategy = FieldStrategy.ALWAYS)
    private Integer timeoutSeconds;
}
