package com.iceeboot.framework.vo.agent;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/** 工具响应视图。 */
@Data
@Schema(description = "工具响应视图")
public class AgentToolVO {
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

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
