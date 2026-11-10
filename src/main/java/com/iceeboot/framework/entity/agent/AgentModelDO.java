package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.JdbcType;

/** AgentModel 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_model")
@Schema(description = "模型连接实体")
public class AgentModelDO extends IceeBootDO {
    @Schema(description = "连接名称")
    @TableField(value = "name", updateStrategy = FieldStrategy.ALWAYS)
    private String name;

    @Schema(description = "模型供应商")
    @TableField(value = "provider", updateStrategy = FieldStrategy.ALWAYS)
    private String provider;

    @Schema(description = "调用协议")
    @TableField(value = "protocol", updateStrategy = FieldStrategy.ALWAYS)
    private String protocol;

    @Schema(description = "模型服务地址")
    @TableField(value = "base_url", updateStrategy = FieldStrategy.ALWAYS)
    private String baseUrl;

    @Schema(description = "加密 API Key")
    @TableField(
            value = "api_key",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String apiKey;

    @Schema(description = "模型名称")
    @TableField(value = "model", updateStrategy = FieldStrategy.ALWAYS)
    private String model;

    @Schema(description = "上下文窗口")
    @TableField(value = "context_window", updateStrategy = FieldStrategy.ALWAYS)
    private Integer contextWindow;

    @Schema(description = "是否启用")
    @TableField(value = "enabled", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean enabled;

    @Schema(description = "生成参数 JSON")
    @TableField(
            value = "generation_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String generationJson;
}
