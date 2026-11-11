package com.iceeboot.framework.dto.create.agent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
@Data
@Schema(description = "技能压缩包预览请求")
public class AgentSkillArchiveDTO {
    @NotNull
    @Schema(description = "ZIP、tar.gz 或 tgz 文件，最大 20 MiB")
    private MultipartFile file;
}
