package com.iceeboot.common.service.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.model.*;
import com.iceeboot.common.service.oss.dto.OSSCapacityInfo;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 阿里云OSS服务实现类
 * 使用阿里云官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class AliyunOSSService implements OSSService {
    
    private static final String PROVIDER_NAME = "aliyun";
    private static final String DEFAULT_ENDPOINT = "oss-cn-hangzhou.aliyuncs.com";
    
    private OSS createOSSClient(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        return new OSSClientBuilder().build(endpoint, request.getAccessKeyId(), request.getAccessKeySecret());
    }
    
    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                if (request.getContent() == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ossClient = createOSSClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(ossClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(request.getContent().length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
                PutObjectResult result = ossClient.putObject(request.getBucketName(), request.getObjectKey(), inputStream, metadata);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setEtag(result.getETag())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("阿里云OSS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, byte[] content) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                if (content == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ossClient = createOSSClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(ossClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(content.length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
                PutObjectResult result = ossClient.putObject(request.getBucketName(), request.getObjectKey(), inputStream, metadata);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setEtag(result.getETag())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("阿里云OSS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                if (inputStream == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "输入流不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                byte[] content = inputStream.readAllBytes();
                
                ossClient = createOSSClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(ossClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(content.length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
                PutObjectResult result = ossClient.putObject(request.getBucketName(), request.getObjectKey(), byteInputStream, metadata);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setEtag(result.getETag())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (IOException e) {
                return OSSResponse.failure("INVALID_CONTENT", "读取输入流失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("阿里云OSS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                OSSObject ossObject = ossClient.getObject(request.getBucketName(), request.getObjectKey());
                byte[] content = ossObject.getObjectContent().readAllBytes();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setContent(content)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("阿里云OSS下载文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_FAILED", "下载失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFileToLocal(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                GetObjectRequest getObjectRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
                ossClient.getObject(getObjectRequest, new File(request.getLocalFilePath()));
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("localPath", request.getLocalFilePath()));
                        
            } catch (Exception e) {
                log.error("阿里云OSS下载文件到本地失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_TO_LOCAL_FAILED", "下载到本地失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                ossClient.deleteObject(request.getBucketName(), request.getObjectKey());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("阿里云OSS删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_FAILED", "删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFiles(OSSRequest request, List<String> objectKeys) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(request.getBucketName());
                deleteRequest.setKeys(objectKeys);
                
                DeleteObjectsResult result = ossClient.deleteObjects(deleteRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("deletedCount", result.getDeletedObjects().size()));
                        
            } catch (Exception e) {
                log.error("阿里云OSS批量删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("BATCH_DELETE_FAILED", "批量删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> fileExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                boolean exists = ossClient.doesObjectExist(request.getBucketName(), request.getObjectKey());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", exists));
                        
            } catch (Exception e) {
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", false));
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> getFileInfo(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                ObjectMetadata metadata = ossClient.getObjectMetadata(request.getBucketName(), request.getObjectKey());
                
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("size", metadata.getContentLength());
                fileInfo.put("contentType", metadata.getContentType());
                fileInfo.put("lastModified", metadata.getLastModified());
                fileInfo.put("etag", metadata.getETag());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(fileInfo);
                        
            } catch (Exception e) {
                log.error("阿里云OSS获取文件信息失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GET_FILE_INFO_FAILED", "获取文件信息失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> listFiles(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                ListObjectsRequest listRequest = new ListObjectsRequest(request.getBucketName());
                if (request.getObjectKey() != null) {
                    listRequest.setPrefix(request.getObjectKey());
                }
                
                ObjectListing objectListing = ossClient.listObjects(listRequest);
                
                List<Map<String, Object>> files = objectListing.getObjectSummaries().stream()
                        .map(summary -> {
                            Map<String, Object> file = new HashMap<>();
                            file.put("key", summary.getKey());
                            file.put("size", summary.getSize());
                            file.put("lastModified", summary.getLastModified().toString());
                            return file;
                        })
                        .collect(Collectors.toList());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("files", files));
                        
            } catch (Exception e) {
                log.error("阿里云OSS列出文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("LIST_FILES_FAILED", "列出文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSCapacityInfo> checkCapacity(OSSRequest request) {
        return Mono.fromCallable(() -> {
            // 阿里云OSS没有直接的容量查询API，返回默认值
            OSSCapacityInfo capacityInfo = new OSSCapacityInfo()
                    .setProvider(getProvider())
                    .setBucketName(request.getBucketName())
                    .setTotalCapacity(-1L) // 表示无限制
                    .setUsedCapacity(0L)
                    .setAvailableCapacity(-1L)
                    .setStatisticsTime(LocalDateTime.now());
            return capacityInfo;
        });
    }
    
    @Override
    public Mono<OSSResponse> generatePresignedUrl(OSSRequest request, Long expiration) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                Date expirationDate = new Date(System.currentTimeMillis() + expiration * 1000);
                
                // 创建生成预签名URL的请求
                GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                    request.getBucketName(), 
                    request.getObjectKey(), 
                    HttpMethod.GET
                );
                generatePresignedUrlRequest.setExpiration(expirationDate);
                
                // 如果请求中包含原始文件名，添加Content-Disposition参数以支持中文文件名
                if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
                    try {
                        // 对文件名进行URL编码
                        String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8")
                                .replace("+", "%20"); // 替换空格编码
                        
                        // 添加response-content-disposition查询参数
                        generatePresignedUrlRequest.addQueryParameter("response-content-disposition", 
                            "attachment; filename*=UTF-8''" + encodedFileName);
                    } catch (Exception e) {
                        log.warn("文件名编码失败，使用默认文件名: {}", e.getMessage());
                    }
                }
                
                URL presignedUrl = ossClient.generatePresignedUrl(generatePresignedUrlRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(presignedUrl.toString())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", expirationDate.getTime()));
                        
            } catch (Exception e) {
                log.error("阿里云OSS生成预签名URL失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GENERATE_PRESIGNED_URL_FAILED", "生成预签名URL失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> copyFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                CopyObjectRequest copyRequest = new CopyObjectRequest(request.getBucketName(), sourceObjectKey, 
                        request.getBucketName(), targetObjectKey);
                ossClient.copyObject(copyRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
                        
            } catch (Exception e) {
                log.error("阿里云OSS复制文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("COPY_FILE_FAILED", "复制文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> moveFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return copyFile(request, sourceObjectKey, targetObjectKey)
                .flatMap(copyResponse -> {
                    if (copyResponse.getSuccess()) {
                        OSSRequest deleteRequest = new OSSRequest();
                        deleteRequest.setBucketName(request.getBucketName());
                        deleteRequest.setObjectKey(sourceObjectKey);
                        deleteRequest.setAccessKeyId(request.getAccessKeyId());
                        deleteRequest.setAccessKeySecret(request.getAccessKeySecret());
                        deleteRequest.setEndpoint(request.getEndpoint());
                        
                        return deleteFile(deleteRequest)
                                .map(deleteResponse -> {
                                    if (deleteResponse.getSuccess()) {
                                        return copyResponse.setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey, "moved", true));
                                    } else {
                                        return OSSResponse.failure("MOVE_FILE_FAILED", "移动文件失败：复制成功但删除源文件失败")
                                                .setProvider(getProvider());
                                    }
                                });
                    } else {
                        return Mono.just(OSSResponse.failure("MOVE_FILE_FAILED", "移动文件失败：复制文件失败")
                                .setProvider(getProvider()));
                    }
                });
    }
    
    @Override
    public Mono<OSSResponse> createBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                
                CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
                ossClient.createBucket(createRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("阿里云OSS创建存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("CREATE_BUCKET_FAILED", "创建存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                ossClient.deleteBucket(request.getBucketName());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("阿里云OSS删除存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_BUCKET_FAILED", "删除存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> bucketExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            OSS ossClient = null;
            
            try {
                ossClient = createOSSClient(request);
                boolean exists = ossClient.doesBucketExist(request.getBucketName());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", exists));
                        
            } catch (Exception e) {
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", false));
            } finally {
                if (ossClient != null) {
                    ossClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<Boolean> validateConfig(OSSRequest request) {
        return bucketExists(request)
                .map(response -> response.getSuccess())
                .onErrorReturn(false);
    }
    
    /**
     * 确保桶存在，如果不存在则创建
     * @param ossClient OSS客户端
     * @param request OSS请求
     * @return 操作结果
     */
    private OSSResponse ensureBucketExists(OSS ossClient, OSSRequest request) {
        try {
            // 检查桶是否存在
            boolean bucketExists = ossClient.doesBucketExist(request.getBucketName());
            if (!bucketExists) {
                log.info("桶 {} 不存在，尝试创建", request.getBucketName());
                
                // 尝试创建桶
                try {
                    CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
                    ossClient.createBucket(createRequest);
                    log.info("成功创建桶: {}", request.getBucketName());
                } catch (Exception createException) {
                    String errorMsg = createException.getMessage();
                    log.error("创建桶失败: {}", errorMsg, createException);
                    
                    // 检查是否是桶名已存在的错误
                    if (errorMsg.contains("BucketAlreadyExists") || errorMsg.contains("already exists")) {
                        return OSSResponse.failure("BUCKET_ALREADY_EXISTS", 
                            String.format("桶名 '%s' 已被其他用户使用，请更换桶名", request.getBucketName()))
                            .setProvider(getProvider());
                    } else {
                        return OSSResponse.failure("CREATE_BUCKET_FAILED", 
                            String.format("创建桶失败: %s", errorMsg))
                            .setProvider(getProvider());
                    }
                }
            }
            
            return OSSResponse.success().setProvider(getProvider());
            
        } catch (Exception e) {
            log.error("检查桶存在性失败: {}", e.getMessage(), e);
            return OSSResponse.failure("CHECK_BUCKET_FAILED", "检查桶存在性失败: " + e.getMessage())
                    .setProvider(getProvider());
        }
    }
    
    /**
     * 构建文件URL
     */
    private String buildFileUrl(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
        
        if (request.getCustomDomain() != null) {
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s://%s.%s/%s", protocol, request.getBucketName(), endpoint, request.getObjectKey());
        }
    }
}