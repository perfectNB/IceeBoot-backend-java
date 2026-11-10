package com.iceeboot.framework.dto.update.agent;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.framework.dto.create.agent.AgentSkillDTO;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 技能更新请求。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "技能更新请求")
public class AgentSkillUpdateDTO extends AgentSkillDTO {
    @NotNull(message = ResponseMessage.Agent.ID_REQUIRED)
    @Positive(message = ResponseMessage.Agent.ID_INVALID)
    @Schema(description = "主键ID")
    private Long id;
}
