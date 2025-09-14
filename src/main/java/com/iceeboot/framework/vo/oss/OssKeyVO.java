package com.iceeboot.framework.vo.oss;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OSS密钥视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-05
 */
@Data
@Schema(description = "OSS密钥视图对象")
public class OssKeyVO {

    @Schema(description = "密钥ID")
    private Long id;

    @Schema(description = "OSS厂商")
    private String ossProvider;

    @Schema(description = "Access Key ID")
    private String accessKeyId;

    @Schema(description = "Access Key Secret")
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

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}