/**
 * AI代理实体类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.entity.ai;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
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
@TableName("ai_agent")
public class AiAgentDO extends ZbootDO {

    /**
     * 代理名称
     */
    @TableField("name")
    private String name;

    /**
     * 代理类型（workflow-工作流，agent-代理）
     */
    @TableField("type")
    private String type;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 平台（coze-扣子，dify-Dify）
     */
    @TableField("platform")
    private String platform;

    /**
     * 状态（active-激活，inactive-禁用）
     */
    @TableField("status")
    private String status;

    /**
     * Coze平台令牌
     */
    @TableField("token")
    private String token;

    /**
     * Coze机器人ID
     */
    @TableField("bot_id")
    private String botId;

    /**
     * Coze工作流ID
     */
    @TableField("work_flow_id")
    private String workFlowId;

    /**
     * Dify平台密钥
     */
    @TableField("key")
    private String key;
}