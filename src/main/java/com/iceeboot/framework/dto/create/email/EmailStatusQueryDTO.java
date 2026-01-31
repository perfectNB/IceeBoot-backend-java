package com.iceeboot.framework.dto.create.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "邮件状态查询请求")
public class EmailStatusQueryDTO {
    @Schema(description = "消息ID")
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @Schema(description = "密钥ID")
    private Long keyId;

    @Schema(description = "Email厂商")
    private String provider;
}
