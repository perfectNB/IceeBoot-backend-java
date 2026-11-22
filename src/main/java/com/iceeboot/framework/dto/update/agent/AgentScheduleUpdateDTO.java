package com.iceeboot.framework.dto.update.agent;

import com.iceeboot.framework.dto.create.agent.AgentScheduleDTO;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AgentScheduleUpdateDTO extends AgentScheduleDTO {
    @NotNull @Positive
    private Long id;
}
