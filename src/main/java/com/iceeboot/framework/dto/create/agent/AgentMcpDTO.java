package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.util.List;
import java.util.Map;

/** MCP 服务请求参数。 */
@Data
@Schema(description = "MCP 服务请求参数")
public class AgentMcpDTO {
    @Schema(description = "编码")
    @NotBlank
    @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}")
    private String code;

    @Schema(description = "名称")
    @NotBlank
    @Size(max = 120)
    private String name;

    @Schema(description = "说明")
    @Size(max = 2000)
    private String description;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "服务地址")
    @NotBlank
    private String url;

    @Schema(description = "请求头，响应中的敏感值已脱敏")
    private Map<String, String> headers;

    @Schema(description = "MCP 工具白名单")
    private List<String> allowedTools;
}
