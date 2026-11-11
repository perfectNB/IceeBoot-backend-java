package com.iceeboot.framework.dto.create.agent;
import lombok.Data;
import jakarta.validation.constraints.Size;
@Data
public class AgentSessionDTO {
    @Size(max=120)
    private String title;
}
