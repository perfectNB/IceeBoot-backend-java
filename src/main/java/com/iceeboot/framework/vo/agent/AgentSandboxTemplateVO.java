package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.*;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;

import lombok.Data;

/** 沙箱模板响应视图。 */
@Data
@lombok.EqualsAndHashCode(callSuper = true)
@Schema(description = "沙箱模板响应视图")
public class AgentSandboxTemplateVO extends AgentResourceVO {
    @Schema(description = "E2B 模板标识")
    private String templateId;

    @Schema(description = "沙箱超时秒数")
    private Integer timeoutSeconds;
}
