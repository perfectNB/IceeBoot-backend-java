package com.iceeboot.framework.dto.update.agent;

import jakarta.validation.constraints.NotNull;

public record AgentScheduleStatusDTO(@NotNull Boolean enabled) {}
