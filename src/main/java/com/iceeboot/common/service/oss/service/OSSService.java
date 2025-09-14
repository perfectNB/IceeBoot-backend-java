/**
 * OSS服务抽象接口
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.oss.service;

import com.iceeboot.common.service.oss.dto.OSSCapacityInfo;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;

/**
 * OSS服务抽象接口
 * 定义了对象存储的基本操作：上传、下载、删除、容量检查等
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
public interface OSSService {
    
    /**
     * 获取厂商名称
     * @return 厂商名称
     */
    String getProvider();
    
    /**
     * 上传文件
     * @param request 上传请求
     * @return 上传响应
     */
    Mono<OSSResponse> uploadFile(OSSRequest request);
    
    /**
     * 上传文件（字节数组）
     * @param request 上传请求
     * @return 上传响应
     */
    Mono<OSSResponse> uploadFile(OSSRequest request, byte[] content);
    
    /**
     * 上传文件（输入流）
     * @param request 上传请求
     * @param inputStream 文件输入流
     * @return 上传响应
     */
    Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream);
    
    /**
     * 下载文件
     * @param request 下载请求
     * @return 下载响应
     */
    Mono<OSSResponse> downloadFile(OSSRequest request);
    
    /**
     * 下载文件到本地
     * @param request 下载请求
     * @return 下载响应
     */
    Mono<OSSResponse> downloadFileToLocal(OSSRequest request);
    
    /**
     * 删除文件
     * @param request 删除请求
     * @return 删除响应
     */
    Mono<OSSResponse> deleteFile(OSSRequest request);
    
    /**
     * 批量删除文件
     * @param request 删除请求（包含多个对象键）
     * @param objectKeys 对象键列表
     * @return 删除响应
     */
    Mono<OSSResponse> deleteFiles(OSSRequest request, List<String> objectKeys);
    
    /**
     * 检查文件是否存在
     * @param request 检查请求
     * @return 检查响应
     */
    Mono<OSSResponse> fileExists(OSSRequest request);
    
    /**
     * 获取文件信息
     * @param request 请求
     * @return 文件信息响应
     */
    Mono<OSSResponse> getFileInfo(OSSRequest request);
    
    /**
     * 列出文件
     * @param request 列表请求
     * @return 文件列表响应
     */
    Mono<OSSResponse> listFiles(OSSRequest request);
    
    /**
     * 检查存储桶容量
     * @param request 容量检查请求
     * @return 容量信息
     */
    Mono<OSSCapacityInfo> checkCapacity(OSSRequest request);
    
    /**
     * 生成预签名URL
     * @param request 请求
     * @param expiration 过期时间（秒）
     * @return 预签名URL响应
     */
    Mono<OSSResponse> generatePresignedUrl(OSSRequest request, Long expiration);
    
    /**
     * 复制文件
     * @param request 复制请求
     * @param sourceObjectKey 源对象键
     * @param targetObjectKey 目标对象键
     * @return 复制响应
     */
    Mono<OSSResponse> copyFile(OSSRequest request, String sourceObjectKey, String targetObjectKey);
    
    /**
     * 移动文件
     * @param request 移动请求
     * @param sourceObjectKey 源对象键
     * @param targetObjectKey 目标对象键
     * @return 移动响应
     */
    Mono<OSSResponse> moveFile(OSSRequest request, String sourceObjectKey, String targetObjectKey);
    
    /**
     * 创建存储桶
     * @param request 创建请求
     * @return 创建响应
     */
    Mono<OSSResponse> createBucket(OSSRequest request);
    
    /**
     * 删除存储桶
     * @param request 删除请求
     * @return 删除响应
     */
    Mono<OSSResponse> deleteBucket(OSSRequest request);
    
    /**
     * 检查存储桶是否存在
     * @param request 检查请求
     * @return 检查响应
     */
    Mono<OSSResponse> bucketExists(OSSRequest request);
    
    /**
     * 获取默认配置
     * @return 默认配置
     */
    default OSSRequest getDefaultConfig() {
        return new OSSRequest()
                .setTimeout(60)
                .setUseHttps(true);
    }
    
    /**
     * 验证配置
     * @param request 配置请求
     * @return 验证结果
     */
    Mono<Boolean> validateConfig(OSSRequest request);
    
    /**
     * 服务是否可用
     * @return 是否可用
     */
    default boolean isAvailable() {
        return true;
    }
    
    /**
     * 获取支持的操作列表
     * @return 支持的操作
     */
    default List<String> getSupportedOperations() {
        return List.of("upload", "download", "delete", "list", "capacity", "presigned");
    }
}