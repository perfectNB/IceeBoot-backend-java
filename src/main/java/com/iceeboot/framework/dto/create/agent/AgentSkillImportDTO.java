package com.iceeboot.framework.dto.create.agent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;
@Data
@Schema(description = "确认导入选中的技能，不覆盖已有编码")
public class AgentSkillImportDTO {
    @NotEmpty @Size(max = 50)
    @Valid
    private List<@NotNull AgentSkillDTO> skills;
}
