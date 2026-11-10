package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.Map;

/** AgentToolConfigDTO请求参数。 */
@Data
@Schema(description = "AgentToolConfigDTO")
public class AgentToolConfigDTO {
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
