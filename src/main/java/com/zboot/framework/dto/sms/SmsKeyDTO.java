package com.zboot.framework.dto.sms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * SMS密钥数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Data
@Schema(description = "SMS密钥数据传输对象")
public class SmsKeyDTO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "SMS厂商")
    @NotBlank(message = "SMS厂商不能为空")
    private String smsProvider;

    @Schema(description = "Access Key ID / App Key")
    @NotBlank(message = "Access Key ID不能为空")
    private String accessKeyId;

    @Schema(description = "Access Key Secret / App Secret")
    @NotBlank(message = "Access Key Secret不能为空")
    private String accessKeySecret;

    @Schema(description = "端点地址")
    private String endpoint;

    @Schema(description = "默认签名")
    private String defaultSignName;

    @Schema(description = "默认模板ID")
    private String defaultTemplateId;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "通道号/发送方")
    private String sender;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "扩展配置（JSON格式）")
    private String extraConfig;
}