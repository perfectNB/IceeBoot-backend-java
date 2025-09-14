/**
 * AI代理实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI代理实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ai_agent")
@Schema(description = "Agent")
public class AiAgentDO extends IceeBootDO {

    @Schema(description = "代理名称")
    @TableField("name")
    private String name;

    @Schema(description = "代理类型（workflow-工作流，agent-代理）")
    @TableField("type")
    private String type;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "平台（coze-扣子，dify-Dify）")
    @TableField("platform")
    private String platform;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "Coze平台令牌")
    @TableField("token")
    private String token;

    @Schema(description = "Coze机器人ID")
    @TableField("bot_id")
    private String botId;

    @Schema(description = "Coze工作流ID")
    @TableField("work_flow_id")
    private String workFlowId;

    @Schema(description = "Dify平台密钥")
    @TableField("api_key")
    private String apiKey;
}