/**
 * OSS密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
package com.zboot.framework.entity.oss;

import com.baomidou.mybatisplus.annotation.*;
import com.zboot.common.core.domain.ZbootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSS密钥实体类
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_oss_key")
@Schema(description = "oss密钥")
public class OssKeyDO extends ZbootDO {

    @Schema(description = "OSS厂商")
    @TableField("oss_provider")
    private String ossProvider;

    @Schema(description = "Access Key ID")
    @TableField("access_key_id")
    private String accessKeyId;

    @Schema(description = "Access Key Secret")
    @TableField("access_key_secret")
    private String accessKeySecret;

    @Schema(description = "端点地址")
    @TableField("endpoint")
    private String endpoint;

    @Schema(description = "默认存储桶")
    @TableField("default_bucket")
    private String defaultBucket;

    @Schema(description = "区域")
    @TableField("region")
    private String region;

    @Schema(description = "自定义域名")
    @TableField("custom_domain")
    private String customDomain;

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