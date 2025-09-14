/**
 * OSS文件实体类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.iceeboot.framework.entity.oss;

import com.baomidou.mybatisplus.annotation.*;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSS文件实体类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_oss_file")
@Schema(description = "oss文件")
public class OssFileDO extends IceeBootDO {

    @Schema(description = "文件名")
    @TableField("file_name")
    private String fileName;

    @Schema(description = "文件原始名")
    @TableField("original_name")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    @TableField("file_size")
    private Long fileSize;

    @Schema(description = "文件地址")
    @TableField("file_url")
    private String fileUrl;

    @Schema(description = "文件类型")
    @TableField("file_type")
    private String fileType;

    @Schema(description = "文件存储供应商（aliyun-阿里云，huawei-华为云，tencent-腾讯云，qiniu-七牛云，minio-MinIO）")
    @TableField("provider")
    private String provider;

    @Schema(description = "文件所在桶名")
    @TableField("bucket_name")
    private String bucketName;

    @Schema(description = "文件MD5值")
    @TableField("file_md5")
    private String fileMd5;

    @Schema(description = "文件状态（active-正常，deleted-已删除）")
    @TableField("status")
    private String status;
}