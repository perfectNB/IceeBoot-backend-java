package com.zboot.framework.query.oss;

import com.zboot.common.core.domain.BaseQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSS文件查询对象
 * 
 * @author CodeIcee
 * @date 2025-09-12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "OSS文件查询对象")
public class OssFileQuery extends BaseQuery {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件原始名")
    private String originalName;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "供应商")
    private String provider;

    @Schema(description = "桶名")
    private String bucketName;

    @Schema(description = "文件状态（active-正常，deleted-已删除）")
    private String status;

    @Schema(description = "开始时间")
    private String startTime;

    @Schema(description = "结束时间")
    private String endTime;
}