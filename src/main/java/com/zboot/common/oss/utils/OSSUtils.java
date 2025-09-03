/**
 * OSS工具类
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.oss.utils;

import com.zboot.common.oss.dto.OSSRequest;
import com.zboot.common.oss.exception.OSSException;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * OSS工具类
 * 提供OSS操作相关的通用工具方法
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public class OSSUtils {
    
    /**
     * 支持的图片文件类型
     */
    private static final List<String> IMAGE_TYPES = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico"
    );
    
    /**
     * 支持的文档文件类型
     */
    private static final List<String> DOCUMENT_TYPES = Arrays.asList(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp"
    );
    
    /**
     * 支持的音频文件类型
     */
    private static final List<String> AUDIO_TYPES = Arrays.asList(
            "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a"
    );
    
    /**
     * 支持的视频文件类型
     */
    private static final List<String> VIDEO_TYPES = Arrays.asList(
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "3gp"
    );
    
    /**
     * 默认允许的文件类型
     */
    private static final List<String> DEFAULT_ALLOWED_TYPES = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "xls", "xlsx", "txt"
    );
    
    /**
     * 生成唯一的对象键
     * 
     * @param originalFileName 原始文件名
     * @return 唯一的对象键
     */
    public static String generateUniqueObjectKey(String originalFileName) {
        String extension = getFileExtension(originalFileName);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd/HHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        
        if (extension != null && !extension.isEmpty()) {
            return String.format("%s/%s.%s", timestamp, uuid, extension);
        } else {
            return String.format("%s/%s", timestamp, uuid);
        }
    }
    
    /**
     * 生成基于日期的对象键
     * 
     * @param originalFileName 原始文件名
     * @param prefix 前缀
     * @return 对象键
     */
    public static String generateDateBasedObjectKey(String originalFileName, String prefix) {
        String extension = getFileExtension(originalFileName);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = getFileNameWithoutExtension(originalFileName);
        String timestamp = String.valueOf(System.currentTimeMillis());
        
        StringBuilder keyBuilder = new StringBuilder();
        if (prefix != null && !prefix.trim().isEmpty()) {
            keyBuilder.append(prefix.trim()).append("/");
        }
        keyBuilder.append(date).append("/");
        keyBuilder.append(fileName).append("_").append(timestamp);
        
        if (extension != null && !extension.isEmpty()) {
            keyBuilder.append(".").append(extension);
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param fileName 文件名
     * @return 文件扩展名（不包含点号）
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        
        return null;
    }
    
    /**
     * 获取不包含扩展名的文件名
     * 
     * @param fileName 文件名
     * @return 不包含扩展名的文件名
     */
    public static String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "";
        }
        
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        
        return fileName;
    }
    
    /**
     * 根据文件扩展名推断内容类型
     * 
     * @param fileName 文件名
     * @return 内容类型
     */
    public static String getContentType(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "application/octet-stream";
        }
        
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return "application/octet-stream";
        }
        
        // 使用JDK内置的方法推断内容类型
        String contentType = URLConnection.guessContentTypeFromName(fileName);
        if (contentType != null) {
            return contentType;
        }
        
        // 手动映射常见的文件类型
        switch (extension) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "svg":
                return "image/svg+xml";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "pptx":
                return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            case "xml":
                return "application/xml";
            case "zip":
                return "application/zip";
            case "rar":
                return "application/x-rar-compressed";
            case "7z":
                return "application/x-7z-compressed";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * 验证文件类型是否允许
     * 
     * @param fileName 文件名
     * @param allowedTypes 允许的文件类型列表
     * @return 是否允许
     */
    public static boolean isFileTypeAllowed(String fileName, List<String> allowedTypes) {
        String extension = getFileExtension(fileName);
        if (extension == null) {
            return false;
        }
        
        List<String> typesToCheck = allowedTypes != null ? allowedTypes : DEFAULT_ALLOWED_TYPES;
        return typesToCheck.contains(extension.toLowerCase());
    }
    
    /**
     * 验证文件大小是否在允许范围内
     * 
     * @param fileSize 文件大小（字节）
     * @param maxSize 最大允许大小（字节）
     * @return 是否在允许范围内
     */
    public static boolean isFileSizeAllowed(long fileSize, long maxSize) {
        return fileSize > 0 && fileSize <= maxSize;
    }
    
    /**
     * 判断是否为图片文件
     * 
     * @param fileName 文件名
     * @return 是否为图片文件
     */
    public static boolean isImageFile(String fileName) {
        String extension = getFileExtension(fileName);
        return extension != null && IMAGE_TYPES.contains(extension.toLowerCase());
    }
    
    /**
     * 判断是否为文档文件
     * 
     * @param fileName 文件名
     * @return 是否为文档文件
     */
    public static boolean isDocumentFile(String fileName) {
        String extension = getFileExtension(fileName);
        return extension != null && DOCUMENT_TYPES.contains(extension.toLowerCase());
    }
    
    /**
     * 判断是否为音频文件
     * 
     * @param fileName 文件名
     * @return 是否为音频文件
     */
    public static boolean isAudioFile(String fileName) {
        String extension = getFileExtension(fileName);
        return extension != null && AUDIO_TYPES.contains(extension.toLowerCase());
    }
    
    /**
     * 判断是否为视频文件
     * 
     * @param fileName 文件名
     * @return 是否为视频文件
     */
    public static boolean isVideoFile(String fileName) {
        String extension = getFileExtension(fileName);
        return extension != null && VIDEO_TYPES.contains(extension.toLowerCase());
    }
    
    /**
     * 计算文件的MD5哈希值
     * 
     * @param content 文件内容
     * @return MD5哈希值
     */
    public static String calculateMD5(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("计算MD5失败", e);
            return null;
        }
    }
    
    /**
     * 计算文件的SHA256哈希值
     * 
     * @param content 文件内容
     * @return SHA256哈希值
     */
    public static String calculateSHA256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(content);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("计算SHA256失败", e);
            return null;
        }
    }
    
    /**
     * 将字节数组转换为Base64字符串
     * 
     * @param content 字节数组
     * @return Base64字符串
     */
    public static String encodeBase64(byte[] content) {
        return Base64.getEncoder().encodeToString(content);
    }
    
    /**
     * 将Base64字符串转换为字节数组
     * 
     * @param base64String Base64字符串
     * @return 字节数组
     */
    public static byte[] decodeBase64(String base64String) {
        return Base64.getDecoder().decode(base64String);
    }
    
    /**
     * 从本地文件读取内容
     * 
     * @param filePath 文件路径
     * @return 文件内容
     * @throws IOException 读取异常
     */
    public static byte[] readFileFromLocal(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }
    
    /**
     * 将内容写入本地文件
     * 
     * @param content 文件内容
     * @param filePath 文件路径
     * @throws IOException 写入异常
     */
    public static void writeFileToLocal(byte[] content, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        // 创建父目录
        Files.createDirectories(path.getParent());
        Files.write(path, content);
    }
    
    /**
     * 验证OSS请求参数
     * 
     * @param request OSS请求
     * @throws OSSException 参数验证失败时抛出异常
     */
    public static void validateRequest(OSSRequest request) throws OSSException {
        if (request == null) {
            throw new OSSException("INVALID_REQUEST", "请求参数不能为空");
        }
        
        if (request.getBucketName() == null || request.getBucketName().trim().isEmpty()) {
            throw new OSSException("INVALID_BUCKET_NAME", "存储桶名称不能为空");
        }
        
        if (request.getObjectKey() == null || request.getObjectKey().trim().isEmpty()) {
            throw new OSSException("INVALID_OBJECT_KEY", "对象键不能为空");
        }
        
        if (request.getAccessKeyId() == null || request.getAccessKeyId().trim().isEmpty()) {
            throw new OSSException("INVALID_ACCESS_KEY", "访问密钥ID不能为空");
        }
        
        if (request.getAccessKeySecret() == null || request.getAccessKeySecret().trim().isEmpty()) {
            throw new OSSException("INVALID_SECRET_KEY", "访问密钥Secret不能为空");
        }
    }
    
    /**
     * 验证上传请求参数
     * 
     * @param request OSS请求
     * @throws OSSException 参数验证失败时抛出异常
     */
    public static void validateUploadRequest(OSSRequest request) throws OSSException {
        validateRequest(request);
        
        if (request.getContent() == null && request.getInputStream() == null) {
            throw new OSSException("INVALID_CONTENT", "文件内容不能为空");
        }
        
        // 验证文件大小
        if (request.getContentLength() != null && request.getContentLength() <= 0) {
            throw new OSSException("INVALID_CONTENT_LENGTH", "文件大小必须大于0");
        }
    }
    
    /**
     * 验证下载请求参数
     * 
     * @param request OSS请求
     * @throws OSSException 参数验证失败时抛出异常
     */
    public static void validateDownloadRequest(OSSRequest request) throws OSSException {
        validateRequest(request);
        
        // 如果是下载到本地文件，验证本地文件路径
        if (request.getLocalFilePath() != null && request.getLocalFilePath().trim().isEmpty()) {
            throw new OSSException("INVALID_LOCAL_PATH", "本地文件路径不能为空");
        }
    }
    
    /**
     * 格式化文件大小
     * 
     * @param bytes 字节数
     * @return 格式化后的文件大小
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
    
    /**
     * 清理对象键（移除不安全字符）
     * 
     * @param objectKey 原始对象键
     * @return 清理后的对象键
     */
    public static String sanitizeObjectKey(String objectKey) {
        if (objectKey == null || objectKey.trim().isEmpty()) {
            return objectKey;
        }
        
        // 移除或替换不安全的字符
        return objectKey
                .replaceAll("[\\\\/:*?\"<>|]", "_") // 替换Windows不允许的字符
                .replaceAll("\\s+", "_") // 替换空白字符
                .replaceAll("_{2,}", "_") // 合并多个下划线
                .replaceAll("^_+|_+$", ""); // 移除开头和结尾的下划线
    }
    
    /**
     * 创建输入流从字节数组
     * 
     * @param content 字节数组
     * @return 输入流
     */
    public static InputStream createInputStreamFromBytes(byte[] content) {
        return new ByteArrayInputStream(content);
    }
}