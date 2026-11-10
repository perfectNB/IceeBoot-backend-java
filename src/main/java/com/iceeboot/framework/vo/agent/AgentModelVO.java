package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 模型连接响应视图。 */
@Data
@Schema(description = "模型连接响应视图")
public class AgentModelVO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "名称")
    private String name;

    @Schema(description = "模型供应商")
    private Provider provider;

    @Schema(description = "调用协议")
    private Protocol protocol;

    @Schema(description = "模型服务地址")
    private String baseUrl;

    @Schema(description = "API Key，响应中返回脱敏占位符")
    private String apiKey;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "上下文窗口大小")
    private Integer contextWindow;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "生成参数")
    private AgentGenerationVO generation;
}
