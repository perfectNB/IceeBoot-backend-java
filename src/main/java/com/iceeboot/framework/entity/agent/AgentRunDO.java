package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

/** AgentRun 持久化实体。 */
@Data
@TableName("t_agent_run")
@Schema(description = "智能体执行记录实体")
public class AgentRunDO implements java.io.Serializable {
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "记录 ID")
    private String id;

    @Schema(description = "根任务 ID")
    @TableField(value = "root_run_id")
    private String rootRunId;

    @Schema(description = "父任务 ID")
    @TableField(value = "parent_run_id")
    private String parentRunId;

    @Schema(description = "智能体 ID")
    @TableField(value = "agent_id")
    private Long agentId;

    @Schema(description = "智能体编码")
    @TableField(value = "agent_code")
    private String agentCode;

    @Schema(description = "执行版本")
    @TableField(value = "version")
    private Integer version;

    @Schema(description = "调用用户 ID")
    @TableField(value = "user_id")
    private String userId;

    @Schema(description = "执行状态")
    @TableField(value = "status")
    private String status;

    @Schema(description = "输入")
    @TableField(value = "input_text", jdbcType = JdbcType.LONGVARCHAR)
    private String inputText;

    @Schema(description = "输出")
    @TableField(value = "output_text", jdbcType = JdbcType.LONGVARCHAR)
    private String outputText;

    @Schema(description = "错误信息")
    @TableField(value = "error_text")
    private String errorText;

    @Schema(description = "输入 Token 数")
    @TableField(value = "input_tokens")
    private Long inputTokens;

    @Schema(description = "输出 Token 数")
    @TableField(value = "output_tokens")
    private Long outputTokens;

    @Schema(description = "用量是否已知")
    @TableField(value = "usage_known")
    private Boolean usageKnown;

    @Schema(description = "开始时间")
    @TableField(value = "started_at")
    private LocalDateTime startedAt;

    @Schema(description = "结束时间")
    @TableField(value = "finished_at")
    private LocalDateTime finishedAt;

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
}
