package com.iceeboot.framework.dto.create.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Schema(description = "短信发送请求")
public class SmsSendDTO {
    @Schema(description = "密钥ID")
    private Long keyId;

    @Schema(description = "SMS厂商")
    private String provider;

    @Schema(description = "手机号")
    private String phoneNumber;

    @Schema(description = "手机号列表")
    private List<String> phoneNumbers;

    @Schema(description = "短信内容")
    private String content;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "模板参数")
    private Map<String, String> templateParams;

    @Schema(description = "签名")
    private String signName;

    @Schema(description = "短信类型")
    private String smsType;

    @Schema(description = "国际区号")
    private String countryCode;

    @Schema(description = "定时发送时间戳")
    private Long scheduleTime;

    @Schema(description = "短信有效期（分钟）")
    private Integer validityPeriod;

    @Schema(description = "回调URL")
    private String callbackUrl;

    @Schema(description = "自定义数据")
    private String userData;

    @Schema(description = "发送类型")
    private String sendType;
}
