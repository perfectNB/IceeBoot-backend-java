package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.JdbcType;

/** AgentMcp 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_mcp")
@Schema(description = "MCP 服务实体")
public class AgentMcpDO extends IceeBootDO {
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

    @Schema(description = "MCP 服务地址")
    @TableField(value = "url", updateStrategy = FieldStrategy.ALWAYS)
    private String url;

    @Schema(description = "加密请求头 JSON")
    @TableField(
            value = "headers_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String headersJson;

    @Schema(description = "允许调用的工具 JSON")
    @TableField(
            value = "allowed_tools_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String allowedToolsJson;
}
