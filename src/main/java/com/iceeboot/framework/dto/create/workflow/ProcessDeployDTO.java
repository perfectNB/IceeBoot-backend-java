package com.iceeboot.framework.dto.create.workflow;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "流程部署请求")
public class ProcessDeployDTO {
    @Schema(description = "流程文件")
    private MultipartFile file;

    @Schema(description = "流程名称")
    private String name;

    @Schema(description = "流程分类")
    private String category;

    @Schema(description = "租户ID")
    private String tenantId;
}
