package com.iceeboot.framework.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI代理视图对象
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Schema(description = "AI代理视图对象")
public class AiAgentVO {

    @Schema(description = "代理ID")
    private Long id;

    @Schema(description = "代理名称")
    private String name;

    @Schema(description = "代理类型（workflow-工作流，agent-代理）")
    private String type;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "平台（coze-扣子，dify-Dify）")
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

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}