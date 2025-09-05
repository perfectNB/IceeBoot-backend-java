package com.zboot.framework.dto.create.ai;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * AI代理数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI代理数据传输对象")
public class AiAgentDTO {

    @Schema(description = "代理名称")
    @NotBlank(message = "代理名称不能为空")
    private String name;

    @Schema(description = "代理类型（workflow-工作流，agent-代理）")
    @NotBlank(message = "代理类型不能为空")
    private String type;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "平台（coze-扣子，dify-Dify）")
    @NotBlank(message = "平台不能为空")
    private String platform;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "Coze平台令牌")
    private String token;

    @Schema(description = "Coze机器人ID")
    private String botId;

    @Schema(description = "Coze工作流ID")
    private String workFlowId;

    @Schema(description = "Dify平台密钥")
    private String apiKey;
}