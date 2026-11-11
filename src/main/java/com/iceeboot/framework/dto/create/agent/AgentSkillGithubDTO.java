package com.iceeboot.framework.dto.create.agent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
@Schema(description = "GitHub 技能预览请求")
public class AgentSkillGithubDTO {
    @NotBlank @Size(max = 1000)
    @Schema(description = "公开 GitHub 仓库或 tree 目录地址")
    private String url;
    @Size(max = 200)
    @Schema(description = "可选分支、Tag 或提交 SHA；覆盖 tree 链接中的分支")
    private String ref;
    @Size(max = 500)
    @Schema(description = "可选仓库相对目录；覆盖 tree 链接中的目录")
    private String path;
}
