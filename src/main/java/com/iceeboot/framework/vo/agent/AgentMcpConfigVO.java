package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** AgentMcpConfigVO响应视图，敏感字段由服务脱敏。 */
@Data
@Schema(description = "AgentMcpConfigVO")
public class AgentMcpConfigVO {
    @Schema(description = "服务地址")
    private String url;

    @Schema(description = "请求头，响应中的敏感值已脱敏")
    private Map<String, String> headers;

    @Schema(description = "MCP 工具白名单")
    private List<String> allowedTools;
}
