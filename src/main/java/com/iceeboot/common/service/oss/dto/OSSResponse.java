package com.iceeboot.common.service.oss.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * OSS响应DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class OSSResponse {
    
    /**
     * 操作是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 文件URL
     */
    private String fileUrl;
    
    /**
     * 文件ETag
     */
    private String etag;
    
    /**
     * 文件大小
     */
    private Long fileSize;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
    
    /**
     * 文件输入流（用于下载）
     */
    private InputStream inputStream;
    
    /**
     * 文件内容字节数组
     */
    private byte[] content;
    
    /**
     * 元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象键
     */
    private String objectKey;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTime;
    
    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;
    
    /**
     * 创建成功响应
     */
    public static OSSResponse success() {
        return new OSSResponse().setSuccess(true);
    }
    
    /**
     * 创建失败响应
     */
    public static OSSResponse failure(String errorMessage) {
        return new OSSResponse()
                .setSuccess(false)
                .setErrorMessage(errorMessage);
    }
    
    /**
     * 创建失败响应
     */
    public static OSSResponse failure(String errorCode, String errorMessage) {
        return new OSSResponse()
                .setSuccess(false)
                .setErrorCode(errorCode)
                .setErrorMessage(errorMessage);
    }
}