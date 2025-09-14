/**
 * OSS请求DTO
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.oss.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.InputStream;
import java.util.Map;

/**
 * OSS请求DTO
 * @author CodeIcee
 * @date 2025-09-03
 */
@Data
@Accessors(chain = true)
public class OSSRequest {
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象键（文件路径）
     */
    private String objectKey;
    
    /**
     * 文件输入流
     */
    private InputStream inputStream;
    
    /**
     * 文件内容字节数组
     */
    private byte[] content;
    
    /**
     * 文件大小
     */
    private Long contentLength;
    
    /**
     * 内容类型
     */
    private String contentType;
    
    /**
     * 访问密钥ID
     */
    private String accessKeyId;
    
    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 端点URL
     */
    private String endpoint;
    
    /**
     * 本地文件路径（用于下载）
     */
    private String localFilePath;
    
    /**
     * 元数据
     */
    private Map<String, String> metadata;
    
    /**
     * 扩展参数
     */
    private Map<String, Object> extraParams;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeout;
    
    /**
     * 是否使用HTTPS
     */
    private Boolean useHttps;
    
    /**
     * 自定义域名
     */
    private String customDomain;
    
    /**
     * 原始文件名（用于下载时显示正确的文件名）
     */
    private String originalFileName;
}