package com.zboot.framework.vo.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Email厂商视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "Email厂商视图对象")
public class EmailProviderVO {

    @Schema(description = "厂商名称")
    private String providerName;

    @Schema(description = "厂商显示名称")
    private String displayName;

    @Schema(description = "密钥列表")
    private List<EmailKeyVO> keys;

    @Schema(description = "密钥数量")
    private Integer keyCount;

    @Schema(description = "激活密钥数量")
    private Integer activeKeyCount;

    @Schema(description = "默认SMTP主机")
    private String defaultSmtpHost;

    @Schema(description = "默认SMTP端口")
    private Integer defaultSmtpPort;

    @Schema(description = "支持的邮件类型")
    private List<String> supportedEmailTypes;

    @Schema(description = "支持的功能")
    private List<String> supportedFeatures;
}