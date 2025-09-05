package com.zboot.framework.dto.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * OSS密钥数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Data
@Schema(description = "OSS密钥数据传输对象")
public class OssKeyDTO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "OSS厂商")
    @NotBlank(message = "OSS厂商不能为空")
    private String ossProvider;

    @Schema(description = "Access Key ID")
    @NotBlank(message = "Access Key ID不能为空")
    private String accessKeyId;

    @Schema(description = "Access Key Secret")
    @NotBlank(message = "Access Key Secret不能为空")
    private String accessKeySecret;

    @Schema(description = "端点地址")
    private String endpoint;

    @Schema(description = "默认存储桶")
    private String defaultBucket;

    @Schema(description = "区域")
    private String region;

    @Schema(description = "自定义域名")
    private String customDomain;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "扩展配置（JSON格式）")
    private String extraConfig;
}