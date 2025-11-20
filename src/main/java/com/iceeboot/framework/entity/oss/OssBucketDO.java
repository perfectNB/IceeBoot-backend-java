/**
 * OSS存储桶实体类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
package com.iceeboot.framework.entity.oss;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.iceeboot.common.core.domain.IceeBootDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSS存储桶实体类
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_oss_bucket")
@Schema(description = "oss桶")
public class OssBucketDO extends IceeBootDO {

    @Schema(description = "桶名")
    @TableField("bucket_name")
    private String bucketName;

    @Schema(description = "供应商（aliyun-阿里云，huawei-华为云，tencent-腾讯云，qiniu-七牛云，minio-MinIO）")
    @TableField("provider")
    private String provider;

    @Schema(description = "密钥ID")
    @TableField("key_id")
    private Long keyId;

    @Schema(description = "桶描述")
    @TableField("description")
    private String description;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    @TableField("status")
    private String status;
}