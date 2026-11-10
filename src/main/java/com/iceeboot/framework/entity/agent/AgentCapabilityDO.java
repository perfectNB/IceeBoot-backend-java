package com.iceeboot.framework.entity.agent;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** AgentCapability 持久化实体。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_capability")
@Schema(description = "能力注册实体")
public class AgentCapabilityDO extends IceeBootDO {
    @Schema(description = "资源类型")
    @TableField(value = "kind")
    private String kind;

    @Schema(description = "资源编码")
    @TableField(value = "code")
    private String code;
}
