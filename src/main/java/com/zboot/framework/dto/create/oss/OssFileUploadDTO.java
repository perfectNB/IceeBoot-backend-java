package com.zboot.framework.dto.create.oss;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * OSS文件上传数据传输对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@Schema(description = "OSS文件上传数据传输对象")
public class OssFileUploadDTO {

    @Schema(description = "供应商（aliyun-阿里云，huawei-华为云，tencent-腾讯云，qiniu-七牛云，minio-MinIO）")
    @NotBlank(message = "供应商不能为空")
    private String provider;

    @Schema(description = "密钥ID")
    @NotNull(message = "密钥ID不能为空")
    private Long keyId;

    @Schema(description = "桶名")
    @NotBlank(message = "桶名不能为空")
    private String bucketName;

    @Schema(description = "上传文件")
    @NotNull(message = "上传文件不能为空")
    private MultipartFile file;

    @Schema(description = "文件路径前缀（可选）")
    private String pathPrefix;
}