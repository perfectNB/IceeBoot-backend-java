package com.iceeboot.framework.vo.agent;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** MCP 服务响应视图。 */
@Data
@Schema(description = "MCP 服务响应视图")
public class AgentMcpVO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "编码")
    private String code;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "说明")
    private String description;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "服务地址")
    private String url;

    @Schema(description = "请求头，响应中的敏感值已脱敏")
    private Map<String, String> headers;

    @Schema(description = "MCP 工具白名单")
    private List<String> allowedTools;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
