package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;

/** AgentRelease 持久化实体。 */
@Data
@TableName("t_agent_release")
@Schema(description = "智能体发布版本实体")
public class AgentReleaseDO implements java.io.Serializable {
    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "记录 ID")
    private String id;

    @Schema(description = "智能体 ID")
    @TableField(value = "agent_id")
    private Long agentId;

    @Schema(description = "发布版本")
    @TableField(value = "version")
    private Integer version;

    @Schema(description = "不可变发布快照 JSON")
    @TableField(value = "snapshot_json", jdbcType = JdbcType.LONGVARCHAR)
    private String snapshotJson;

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
