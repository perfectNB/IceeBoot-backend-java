package com.iceeboot.framework.dto.create.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Email密钥数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "Email密钥数据传输对象")
public class EmailKeyDTO {

    @Schema(description = "Email厂商")
    @NotBlank(message = "Email厂商不能为空")
    private String emailProvider;

    @Schema(description = "邮箱地址")
    @NotBlank(message = "邮箱地址不能为空")
    @Email(message = "邮箱地址格式不正确")
    private String emailAddress;

    @Schema(description = "邮箱密码/授权码")
    @NotBlank(message = "邮箱密码不能为空")
    private String emailPassword;

    @Schema(description = "SMTP服务器地址")
    private String smtpHost;

    @Schema(description = "SMTP端口")
    private Integer smtpPort;

    @Schema(description = "是否启用SSL")
    private Boolean enableSsl;

    @Schema(description = "是否启用TLS")
    private Boolean enableTls;

    @Schema(description = "是否启用认证")
    private Boolean enableAuth;

    @Schema(description = "发件人名称")
    private String senderName;

    @Schema(description = "连接超时时间（毫秒）")
    private Integer connectionTimeout;

    @Schema(description = "读取超时时间（毫秒）")
    private Integer readTimeout;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "扩展配置（JSON格式）")
    private String extraConfig;
}