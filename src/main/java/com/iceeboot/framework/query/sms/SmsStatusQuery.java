package com.iceeboot.framework.query.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "短信状态查询请求")
public class SmsStatusQuery {
    @Schema(description = "消息ID")
    @NotBlank(message = "消息ID不能为空")
    private String messageId;

    @Schema(description = "密钥ID")
    private Long keyId;

    @Schema(description = "SMS厂商")
    private String provider;
}
