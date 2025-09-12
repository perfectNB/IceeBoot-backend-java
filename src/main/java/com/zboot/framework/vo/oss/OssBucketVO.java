package com.zboot.framework.vo.oss;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OSS存储桶视图对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@Schema(description = "OSS存储桶视图对象")
public class OssBucketVO {

    @Schema(description = "桶ID")
    private Long id;

    @Schema(description = "桶名")
    private String bucketName;

    @Schema(description = "供应商")
    private String provider;

    @Schema(description = "密钥ID")
    private Long keyId;

    @Schema(description = "桶描述")
    private String description;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "文件数量")
    private Long fileCount;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}