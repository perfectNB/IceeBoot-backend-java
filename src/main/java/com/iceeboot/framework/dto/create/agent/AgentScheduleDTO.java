package com.iceeboot.framework.dto.create.agent;

import com.iceeboot.framework.enums.agent.AgentScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "智能体定时任务请求，不允许客户端指定执行身份")
public class AgentScheduleDTO {
    @NotBlank @Size(max = 80)
    private String name;
    @NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}")
    private String agentCode;
    @NotBlank @Size(max = 100000)
    private String input;
    @NotNull
    private AgentScheduleType scheduleType;
    @Size(max = 120)
    private String cronExpression;
    @NotBlank @Size(max = 64)
    private String zoneId;
    private Long executeAt;
    @NotNull
    private Boolean enabled;
}
