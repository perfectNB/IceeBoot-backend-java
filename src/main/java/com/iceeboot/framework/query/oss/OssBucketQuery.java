package com.iceeboot.framework.query.oss;

import com.iceeboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSS存储桶查询对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OSS存储桶查询对象")
public class OssBucketQuery extends BaseQuery {

    @Schema(description = "桶名")
    private String bucketName;

    @Schema(description = "供应商")
    private String provider;

    @Schema(description = "状态（active-激活，inactive-禁用）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}