package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_agent_schema_history")
@Schema(description = "已完成的数据库迁移与内容校验和")
public class AgentSchemaHistoryDO {
    @TableId(value = "version", type = IdType.INPUT)
    private Integer version;

    private String description;
    private String checksum;

    @TableField("installed_at")
    private LocalDateTime installedAt;
}
