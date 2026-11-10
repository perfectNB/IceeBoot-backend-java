package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.Map;

/** 知识库连接响应视图。 */
@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库连接响应视图")
public class AgentKnowledgeVO extends AgentResourceVO {
    @Schema(description = "类型")
    private ToolKind kind;

    @Schema(description = "服务地址")
    private String url;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "请求头，响应中的敏感值已脱敏")
    private Map<String, String> headers;

    @Schema(description = "工具参数 JSON Schema")
    private Map<String, Object> inputSchema;

    @Schema(description = "沙箱脚本命令")
    private String script;

    @Schema(description = "是否调用项目内部接口")
    private boolean projectApi;

    @Schema(description = "内部接口权限码")
    private String permissionCode;
}
