/**
 * SMS密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
package com.iceeboot.framework.entity.sms;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SMS密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_sms_key")
@Schema(description = "短信密钥")
public class SmsKeyDO extends IceeBootDO {

    @Schema(description = "SMS厂商")
    @TableField("sms_provider")
    private String smsProvider;

    @Schema(description = "Access Key ID / App Key")
    @TableField("access_key_id")
    private String accessKeyId;

    @Schema(description = "Access Key Secret / App Secret")
    @TableField("access_key_secret")
    private String accessKeySecret;

    @Schema(description = "端点地址")
    @TableField("endpoint")
    private String endpoint;

    @Schema(description = "默认签名")
    @TableField("default_sign_name")
    private String defaultSignName;

    @Schema(description = "默认模板ID")
    @TableField("default_template_id")
    private String defaultTemplateId;

    @Schema(description = "区域")
    @TableField("region")
    private String region;

    @Schema(description = "通道号/发送方")
    @TableField("sender")
    private String sender;

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