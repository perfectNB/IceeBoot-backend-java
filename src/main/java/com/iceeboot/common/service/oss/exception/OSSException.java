/**
 * OSS异常类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.oss.exception;

/**
 * OSS异常类
 * 用于处理OSS操作过程中的各种异常情况
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
public class OSSException extends RuntimeException {
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 厂商名称
     */
    private String provider;
    
    /**
     * 操作类型
     */
    private String operation;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 对象键
     */
    private String objectKey;
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     */
    public OSSException(String message) {
        super(message);
    }
    
    /**
     * 构造函数
     * 
     * @param message 异常消息
     * @param cause 原因
     */
    public OSSException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     */
    public OSSException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     * @param cause 原因
     */
    public OSSException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     * @param provider 厂商名称
     * @param operation 操作类型
     */
    public OSSException(String errorCode, String message, String provider, String operation) {
        super(message);
        this.errorCode = errorCode;
        this.provider = provider;
        this.operation = operation;
    }
    
    /**
     * 构造函数
     * 
     * @param errorCode 错误代码
     * @param message 异常消息
     * @param provider 厂商名称
     * @param operation 操作类型
     * @param cause 原因
     */
    public OSSException(String errorCode, String message, String provider, String operation, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.provider = provider;
        this.operation = operation;
    }
    
    // Getter和Setter方法
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public OSSException setErrorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public OSSException setProvider(String provider) {
        this.provider = provider;
        return this;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public OSSException setOperation(String operation) {
        this.operation = operation;
        return this;
    }
    
    public String getBucketName() {
        return bucketName;
    }
    
    public OSSException setBucketName(String bucketName) {
        this.bucketName = bucketName;
        return this;
    }
    
    public String getObjectKey() {
        return objectKey;
    }
    
    public OSSException setObjectKey(String objectKey) {
        this.objectKey = objectKey;
        return this;
    }
    
    /**
     * 获取详细的异常信息
     * 
     * @return 详细异常信息
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        
        if (provider != null) {
            sb.append("[厂商: ").append(provider).append("] ");
        }
        
        if (operation != null) {
            sb.append("[操作: ").append(operation).append("] ");
        }
        
        if (bucketName != null) {
            sb.append("[存储桶: ").append(bucketName).append("] ");
        }
        
        if (objectKey != null) {
            sb.append("[对象: ").append(objectKey).append("] ");
        }
        
        if (errorCode != null) {
            sb.append("[错误代码: ").append(errorCode).append("] ");
        }
        
        sb.append(getMessage());
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return "OSSException{" +
                "errorCode='" + errorCode + '\'' +
                ", provider='" + provider + '\'' +
                ", operation='" + operation + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", objectKey='" + objectKey + '\'' +
                ", message='" + getMessage() + '\'' +
                '}';
    }
    
    /**
     * 常用的OSS异常工厂方法
     */
    public static class Factory {
        
        /**
         * 创建上传失败异常
         */
        public static OSSException uploadFailed(String provider, String bucketName, String objectKey, String message) {
            return new OSSException("UPLOAD_FAILED", message, provider, "upload")
                    .setBucketName(bucketName)
                    .setObjectKey(objectKey);
        }
        
        /**
         * 创建下载失败异常
         */
        public static OSSException downloadFailed(String provider, String bucketName, String objectKey, String message) {
            return new OSSException("DOWNLOAD_FAILED", message, provider, "download")
                    .setBucketName(bucketName)
                    .setObjectKey(objectKey);
        }
        
        /**
         * 创建删除失败异常
         */
        public static OSSException deleteFailed(String provider, String bucketName, String objectKey, String message) {
            return new OSSException("DELETE_FAILED", message, provider, "delete")
                    .setBucketName(bucketName)
                    .setObjectKey(objectKey);
        }
        
        /**
         * 创建文件不存在异常
         */
        public static OSSException fileNotFound(String provider, String bucketName, String objectKey) {
            return new OSSException("FILE_NOT_FOUND", "文件不存在", provider, "check")
                    .setBucketName(bucketName)
                    .setObjectKey(objectKey);
        }
        
        /**
         * 创建存储桶不存在异常
         */
        public static OSSException bucketNotFound(String provider, String bucketName) {
            return new OSSException("BUCKET_NOT_FOUND", "存储桶不存在", provider, "check")
                    .setBucketName(bucketName);
        }
        
        /**
         * 创建认证失败异常
         */
        public static OSSException authenticationFailed(String provider, String message) {
            return new OSSException("AUTHENTICATION_FAILED", message, provider, "auth");
        }
        
        /**
         * 创建权限不足异常
         */
        public static OSSException accessDenied(String provider, String operation, String message) {
            return new OSSException("ACCESS_DENIED", message, provider, operation);
        }
        
        /**
         * 创建配置错误异常
         */
        public static OSSException configurationError(String provider, String message) {
            return new OSSException("CONFIGURATION_ERROR", message, provider, "config");
        }
        
        /**
         * 创建网络错误异常
         */
        public static OSSException networkError(String provider, String operation, String message) {
            return new OSSException("NETWORK_ERROR", message, provider, operation);
        }
        
        /**
         * 创建超时异常
         */
        public static OSSException timeout(String provider, String operation, String message) {
            return new OSSException("TIMEOUT", message, provider, operation);
        }
        
        /**
         * 创建文件大小超限异常
         */
        public static OSSException fileSizeExceeded(String provider, long fileSize, long maxSize) {
            String message = String.format("文件大小超限：%d字节，最大允许：%d字节", fileSize, maxSize);
            return new OSSException("FILE_SIZE_EXCEEDED", message, provider, "upload");
        }
        
        /**
         * 创建文件类型不支持异常
         */
        public static OSSException unsupportedFileType(String provider, String fileType) {
            String message = String.format("不支持的文件类型：%s", fileType);
            return new OSSException("UNSUPPORTED_FILE_TYPE", message, provider, "upload");
        }
        
        /**
         * 创建厂商不支持异常
         */
        public static OSSException unsupportedProvider(String provider) {
            String message = String.format("不支持的OSS厂商：%s", provider);
            return new OSSException("UNSUPPORTED_PROVIDER", message, provider, "init");
        }
        
        /**
         * 创建操作不支持异常
         */
        public static OSSException unsupportedOperation(String provider, String operation) {
            String message = String.format("厂商 %s 不支持操作：%s", provider, operation);
            return new OSSException("UNSUPPORTED_OPERATION", message, provider, operation);
        }
    }
}