package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Data;

/** 模型连接请求参数。 */
@Data
@Schema(description = "模型连接请求参数")
public class AgentModelDTO {
    @Schema(description = "主键 ID")
    private Long id;

    @Schema(description = "名称")
    @NotBlank
    @Size(max = 120)
    private String name;

    @Schema(description = "模型供应商")
    @NotNull
    private Provider provider;

    @Schema(description = "调用协议")
    @NotNull
    private Protocol protocol;

    @Schema(description = "模型服务地址")
    @NotBlank
    @Size(max = 1000)
    private String baseUrl;

    @Schema(description = "API Key，响应中返回脱敏占位符")
    private String apiKey;

    @Schema(description = "模型名称")
    @NotBlank
    @Size(max = 200)
    private String model;

    @Schema(description = "上下文窗口大小")
    @Min(1024)
    private Integer contextWindow;

    @Schema(description = "是否启用")
    private boolean enabled;

    @Schema(description = "生成参数")
    @Valid
    private AgentGenerationDTO generation;
}
