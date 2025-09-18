package com.iceeboot.common.service.oss.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.region.Region;
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
 * 腾讯云COS服务实现类
 * 使用腾讯云官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class TencentCOSService implements OSSService {
    
    private static final String PROVIDER_NAME = "tencent";
    private static final String DEFAULT_REGION = "ap-beijing";
    
    private COSClient createCOSClient(OSSRequest request) {
        COSCredentials cred = new BasicCOSCredentials(request.getAccessKeyId(), request.getAccessKeySecret());
        String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        return new COSClient(cred, clientConfig);
    }
    
    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                if (request.getContent() == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                cosClient = createCOSClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(cosClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(request.getContent().length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
                PutObjectRequest putRequest = new PutObjectRequest(request.getBucketName(), request.getObjectKey(), inputStream, metadata);
                PutObjectResult result = cosClient.putObject(putRequest);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("腾讯云COS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, byte[] content) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                if (content == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                cosClient = createCOSClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(cosClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(content.length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
                PutObjectRequest putRequest = new PutObjectRequest(request.getBucketName(), request.getObjectKey(), inputStream, metadata);
                PutObjectResult result = cosClient.putObject(putRequest);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("腾讯云COS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                if (inputStream == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "输入流不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                byte[] content = inputStream.readAllBytes();
                
                cosClient = createCOSClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(cosClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(content.length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
                PutObjectRequest putRequest = new PutObjectRequest(request.getBucketName(), request.getObjectKey(), byteInputStream, metadata);
                PutObjectResult result = cosClient.putObject(putRequest);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (IOException e) {
                return OSSResponse.failure("INVALID_CONTENT", "读取输入流失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("腾讯云COS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                GetObjectRequest getRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
                COSObject cosObject = cosClient.getObject(getRequest);
                byte[] content = cosObject.getObjectContent().readAllBytes();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setContent(content)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("腾讯云COS下载文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_FAILED", "下载失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFileToLocal(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                GetObjectRequest getRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
                File localFile = new File(request.getLocalFilePath());
                cosClient.getObject(getRequest, localFile);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("localPath", request.getLocalFilePath()));
                        
            } catch (Exception e) {
                log.error("腾讯云COS下载文件到本地失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_TO_LOCAL_FAILED", "下载到本地失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                cosClient.deleteObject(request.getBucketName(), request.getObjectKey());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("腾讯云COS删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_FAILED", "删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFiles(OSSRequest request, List<String> objectKeys) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(request.getBucketName());
                List<DeleteObjectsRequest.KeyVersion> keys = objectKeys.stream()
                        .map(key -> new DeleteObjectsRequest.KeyVersion(key))
                        .collect(Collectors.toList());
                deleteRequest.setKeys(keys);
                
                DeleteObjectsResult result = cosClient.deleteObjects(deleteRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("deletedCount", result.getDeletedObjects().size()));
                        
            } catch (Exception e) {
                log.error("腾讯云COS批量删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("BATCH_DELETE_FAILED", "批量删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> fileExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                boolean exists = cosClient.doesObjectExist(request.getBucketName(), request.getObjectKey());
                
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
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> getFileInfo(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                ObjectMetadata metadata = cosClient.getObjectMetadata(request.getBucketName(), request.getObjectKey());
                
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
                log.error("腾讯云COS获取文件信息失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GET_FILE_INFO_FAILED", "获取文件信息失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> listFiles(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                ListObjectsRequest listRequest = new ListObjectsRequest();
                listRequest.setBucketName(request.getBucketName());
                if (request.getObjectKey() != null) {
                    listRequest.setPrefix(request.getObjectKey());
                }
                
                ObjectListing objectListing = cosClient.listObjects(listRequest);
                
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
                log.error("腾讯云COS列出文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("LIST_FILES_FAILED", "列出文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSCapacityInfo> checkCapacity(OSSRequest request) {
        return Mono.fromCallable(() -> {
            // 腾讯云COS没有直接的容量查询API，返回默认值
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
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(
                        request.getBucketName(), request.getObjectKey(), HttpMethodName.GET);
                Date expirationDate = new Date(System.currentTimeMillis() + expiration * 1000);
                presignedUrlRequest.setExpiration(expirationDate);
                
                // 添加response-content-disposition参数以正确处理中文文件名
                if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
                    try {
                        String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8");
                        presignedUrlRequest.addRequestParameter("response-content-disposition", "attachment; filename*=UTF-8''" + encodedFileName);
                    } catch (Exception e) {
                        log.warn("编码文件名失败: {}", e.getMessage());
                    }
                }
                
                URL presignedUrl = cosClient.generatePresignedUrl(presignedUrlRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(presignedUrl.toString())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", expirationDate.getTime()));
                        
            } catch (Exception e) {
                log.error("腾讯云COS生成预签名URL失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GENERATE_PRESIGNED_URL_FAILED", "生成预签名URL失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> copyFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                CopyObjectRequest copyRequest = new CopyObjectRequest(
                        request.getBucketName(), sourceObjectKey, request.getBucketName(), targetObjectKey);
                cosClient.copyObject(copyRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
                        
            } catch (Exception e) {
                log.error("腾讯云COS复制文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("COPY_FILE_FAILED", "复制文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
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
                        deleteRequest.setRegion(request.getRegion());
                        
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
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                
                CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
                cosClient.createBucket(createRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("腾讯云COS创建存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("CREATE_BUCKET_FAILED", "创建存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                cosClient.deleteBucket(request.getBucketName());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("腾讯云COS删除存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_BUCKET_FAILED", "删除存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (cosClient != null) {
                    cosClient.shutdown();
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> bucketExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            COSClient cosClient = null;
            
            try {
                cosClient = createCOSClient(request);
                boolean exists = cosClient.doesBucketExist(request.getBucketName());
                
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
                if (cosClient != null) {
                    cosClient.shutdown();
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
     * @param cosClient COS客户端
     * @param request OSS请求
     * @return 操作结果
     */
    private OSSResponse ensureBucketExists(COSClient cosClient, OSSRequest request) {
        try {
            // 检查桶是否存在
            boolean bucketExists = cosClient.doesBucketExist(request.getBucketName());
            if (!bucketExists) {
                log.info("桶 {} 不存在，尝试创建", request.getBucketName());
                
                // 尝试创建桶
                try {
                    CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
                    cosClient.createBucket(createRequest);
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
        String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
        
        if (request.getCustomDomain() != null) {
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s://%s-%s.cos.%s.myqcloud.com/%s", 
                    protocol, request.getBucketName(), request.getAccessKeyId().substring(0, 8), regionName, request.getObjectKey());
        }
    }
}