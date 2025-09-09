package com.zboot.framework.vo.oss;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OSS文件视图对象
 * 
 * @author CodeIcee
 * @date 2025-01-27
 */
@Data
@Schema(description = "OSS文件视图对象")
public class OssFileVO {

    @Schema(description = "文件ID")
    private Long id;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件原始名")
    private String originalName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件地址")
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件存储供应商")
    private String provider;

    @Schema(description = "文件所在桶名")
    private String bucketName;

    @Schema(description = "文件MD5值")
    private String fileMd5;

    @Schema(description = "文件状态（active-正常，deleted-已删除）")
    private String status;

    @Schema(description = "文件大小（格式化）")
    private String fileSizeFormatted;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}