package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

/** AgentModelCall 持久化实体。 */
@Data
@TableName("t_agent_model_call")
@Schema(description = "模型调用记录实体")
public class AgentModelCallDO implements java.io.Serializable {
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "记录 ID")
    private String id;

    @Schema(description = "任务 ID")
    @TableField(value = "run_id")
    private String runId;

    @Schema(description = "根任务 ID")
    @TableField(value = "root_run_id")
    private String rootRunId;

    @Schema(description = "智能体编码")
    @TableField(value = "agent_code")
    private String agentCode;

    @Schema(description = "模型名称")
    @TableField(value = "model")
    private String model;

    @Schema(description = "模型输入")
    @TableField(value = "input_text", jdbcType = JdbcType.LONGVARCHAR)
    private String inputText;

    @Schema(description = "模型输出")
    @TableField(value = "output_text", jdbcType = JdbcType.LONGVARCHAR)
    private String outputText;

    @Schema(description = "输入 Token 数")
    @TableField(value = "input_tokens")
    private Long inputTokens;

    @Schema(description = "输出 Token 数")
    @TableField(value = "output_tokens")
    private Long outputTokens;

    @Schema(description = "用量是否已知")
    @TableField(value = "usage_known")
    private Boolean usageKnown;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("del_flag")
    private Integer delFlag;

    @TableField("cached_tokens")
    private Long cachedTokens;

    private String provider;
    private Long cacheWriteTokens;
    private java.math.BigDecimal estimatedCostCny;
}
