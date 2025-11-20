package com.iceeboot.framework.dto.update.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * OSS存储桶更新数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@Schema(description = "OSS存储桶更新数据传输对象")
public class OssBucketUpdateDTO {

    @Schema(description = "主键ID")
    @NotNull(message = "ID不能为空")
    private Long id;

    @Schema(description = "桶名")
    @NotBlank(message = "桶名不能为空")
    private String bucketName;

    @Schema(description = "供应商（aliyun-阿里云，huawei-华为云，tencent-腾讯云，qiniu-七牛云，minio-MinIO）")
    @NotBlank(message = "供应商不能为空")
    private String provider;

    @Schema(description = "密钥ID")
    @NotNull(message = "密钥ID不能为空")
    private Long keyId;

    @Schema(description = "桶描述")
    private String description;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;
}