/**
 * Email密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
package com.iceeboot.framework.entity.email;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Email密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_email_key")
@Schema(description = "邮箱密钥")
public class EmailKeyDO extends IceeBootDO {

    @Schema(description = "Email厂商")
    @TableField("email_provider")
    private String emailProvider;

    @Schema(description = "邮箱地址")
    @TableField("email_address")
    private String emailAddress;

    @Schema(description = "邮箱密码/授权码")
    @TableField("email_password")
    private String emailPassword;

    @Schema(description = "SMTP服务器地址")
    @TableField("smtp_host")
    private String smtpHost;

    @Schema(description = "SMTP端口")
    @TableField("smtp_port")
    private Integer smtpPort;

    @Schema(description = "是否启用SSL")
    @TableField("enable_ssl")
    private Boolean enableSsl;

    @Schema(description = "是否启用TLS")
    @TableField("enable_tls")
    private Boolean enableTls;

    @Schema(description = "是否启用认证")
    @TableField("enable_auth")
    private Boolean enableAuth;

    @Schema(description = "发件人名称")
    @TableField("sender_name")
    private String senderName;

    @Schema(description = "连接超时时间（毫秒）")
    @TableField("connection_timeout")
    private Integer connectionTimeout;

    @Schema(description = "读取超时时间（毫秒）")
    @TableField("read_timeout")
    private Integer readTimeout;

    @Schema(description = "备注")
    @TableField("remark")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;

    @Schema(description = "扩展配置（JSON格式）")
    @TableField("extra_config")
    private String extraConfig;
}