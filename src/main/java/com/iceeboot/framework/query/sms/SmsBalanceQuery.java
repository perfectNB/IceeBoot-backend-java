package com.iceeboot.framework.query.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "短信余额查询请求")
public class SmsBalanceQuery {
    @Schema(description = "密钥ID")
    private Long keyId;

    @Schema(description = "SMS厂商")
    private String provider;
}
