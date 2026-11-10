package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

import org.apache.ibatis.type.JdbcType;

/** AgentTool 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_tool")
@Schema(description = "工具实体")
public class AgentToolDO extends IceeBootDO {
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

    @Schema(description = "工具执行方式")
    @TableField(value = "kind", updateStrategy = FieldStrategy.ALWAYS)
    private String kind;

    @Schema(description = "调用地址")
    @TableField(value = "url", updateStrategy = FieldStrategy.ALWAYS)
    private String url;

    @Schema(description = "HTTP 方法")
    @TableField(value = "method", updateStrategy = FieldStrategy.ALWAYS)
    private String method;

    @Schema(description = "加密请求头 JSON")
    @TableField(
            value = "headers_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String headersJson;

    @Schema(description = "参数 JSON Schema")
    @TableField(
            value = "input_schema_json",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String inputSchemaJson;

    @Schema(description = "沙箱脚本")
    @TableField(
            value = "script",
            jdbcType = JdbcType.LONGVARCHAR,
            updateStrategy = FieldStrategy.ALWAYS)
    private String script;

    @Schema(description = "是否调用项目接口")
    @TableField(value = "project_api", updateStrategy = FieldStrategy.ALWAYS)
    private Boolean projectApi;

    @Schema(description = "项目接口权限码")
    @TableField(value = "permission_code", updateStrategy = FieldStrategy.ALWAYS)
    private String permissionCode;
}
