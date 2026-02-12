package com.iceeboot.framework.vo.email;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.iceeboot.common.annotation.Sensitive;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Email密钥视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "Email密钥视图对象")
public class EmailKeyVO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "Email厂商")
    private String emailProvider;

    @Schema(description = "邮箱地址")
    private String emailAddress;

    @Sensitive(prefixKeep = 0, suffixKeep = 0)
    @Schema(description = "邮箱密码/授权码")
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

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}