package com.iceeboot.framework.dto.create.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "邮件附件")
public class EmailAttachmentDTO {
    @Schema(description = "附件名称")
    private String fileName;

    @Schema(description = "附件文件路径")
    private String filePath;

    @Schema(description = "附件MIME类型")
    private String mimeType;

    @Schema(description = "附件大小")
    private Long fileSize;

    @Schema(description = "是否内嵌附件")
    private Boolean inline;

    @Schema(description = "内嵌附件ID")
    private String contentId;
}
