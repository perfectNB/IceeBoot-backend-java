/**
 * 华为云OBS服务实现类
 * 使用华为云官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.oss.service.impl;

import com.obs.services.ObsClient;
import com.obs.services.model.*;
import com.zboot.common.service.oss.dto.OSSCapacityInfo;
import com.zboot.common.service.oss.dto.OSSRequest;
import com.zboot.common.service.oss.dto.OSSResponse;
import com.zboot.common.service.oss.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 华为云OBS服务实现类
 * 使用华为云官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class HuaweiOBSService implements OSSService {
    
    private static final String PROVIDER_NAME = "huawei";
    private static final String DEFAULT_ENDPOINT = "obs.cn-north-4.myhuaweicloud.com";
    
    private ObsClient createObsClient(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        return new ObsClient(request.getAccessKeyId(), request.getAccessKeySecret(), endpoint);
    }
    
    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                if (request.getContent() == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                obsClient = createObsClient(request);
                
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength((long) request.getContent().length);
                if (request.getContentType() != null) {
                    metadata.setContentType(request.getContentType());
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
                PutObjectRequest putRequest = new PutObjectRequest();
                putRequest.setBucketName(request.getBucketName());
                putRequest.setObjectKey(request.getObjectKey());
                putRequest.setInput(inputStream);
                putRequest.setMetadata(metadata);
                PutObjectResult result = obsClient.putObject(putRequest);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("华为云OBS上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, byte[] content) {
        request.setContent(content);
        return uploadFile(request);
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream) {
        try {
            byte[] content = inputStream.readAllBytes();
            request.setContent(content);
            return uploadFile(request);
        } catch (IOException e) {
            return Mono.just(OSSResponse.failure("INVALID_CONTENT", "读取输入流失败: " + e.getMessage())
                    .setProvider(getProvider()));
        }
    }
    
    @Override
    public Mono<OSSResponse> downloadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                GetObjectRequest getObjectRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
                ObsObject obsObject = obsClient.getObject(getObjectRequest);
                byte[] content = obsObject.getObjectContent().readAllBytes();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setContent(content)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("华为云OBS下载文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_FAILED", "下载失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFileToLocal(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                GetObjectRequest getObjectRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
                ObsObject obsObject = obsClient.getObject(getObjectRequest);
                
                try (InputStream inputStream = obsObject.getObjectContent();
                     FileOutputStream outputStream = new FileOutputStream(request.getLocalFilePath())) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("localPath", request.getLocalFilePath()));
                        
            } catch (Exception e) {
                log.error("华为云OBS下载文件到本地失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_TO_LOCAL_FAILED", "下载到本地失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                obsClient.deleteObject(request.getBucketName(), request.getObjectKey());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("华为云OBS删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_FAILED", "删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFiles(OSSRequest request, List<String> objectKeys) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(request.getBucketName());
                KeyAndVersion[] keys = objectKeys.stream()
                        .map(key -> new KeyAndVersion(key))
                        .toArray(KeyAndVersion[]::new);
                deleteRequest.setKeyAndVersions(keys);
                
                DeleteObjectsResult result = obsClient.deleteObjects(deleteRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("deletedCount", result.getDeletedObjectResults().size()));
                        
            } catch (Exception e) {
                log.error("华为云OBS批量删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("BATCH_DELETE_FAILED", "批量删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> fileExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                boolean exists = obsClient.doesObjectExist(request.getBucketName(), request.getObjectKey());
                
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
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> getFileInfo(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                ObjectMetadata metadata = obsClient.getObjectMetadata(request.getBucketName(), request.getObjectKey());
                
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("size", metadata.getContentLength());
                fileInfo.put("contentType", metadata.getContentType());
                fileInfo.put("lastModified", metadata.getLastModified());
                fileInfo.put("etag", metadata.getEtag());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(fileInfo);
                        
            } catch (Exception e) {
                log.error("华为云OBS获取文件信息失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GET_FILE_INFO_FAILED", "获取文件信息失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> listFiles(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                ListObjectsRequest listRequest = new ListObjectsRequest(request.getBucketName());
                if (request.getObjectKey() != null) {
                    listRequest.setPrefix(request.getObjectKey());
                }
                
                ObjectListing objectListing = obsClient.listObjects(listRequest);
                
                List<Map<String, Object>> files = objectListing.getObjects().stream()
                        .map(summary -> {
                            Map<String, Object> file = new HashMap<>();
                            file.put("key", summary.getObjectKey());
                            file.put("size", summary.getMetadata().getContentLength());
                            file.put("lastModified", summary.getMetadata().getLastModified().toString());
                            return file;
                        })
                        .collect(Collectors.toList());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("files", files));
                        
            } catch (Exception e) {
                log.error("华为云OBS列出文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("LIST_FILES_FAILED", "列出文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSCapacityInfo> checkCapacity(OSSRequest request) {
        return Mono.fromCallable(() -> {
            // 华为云OBS没有直接的容量查询API，返回默认值
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
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                TemporarySignatureRequest signatureRequest = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration.intValue());
                signatureRequest.setBucketName(request.getBucketName());
                signatureRequest.setObjectKey(request.getObjectKey());
                
                // 添加response-content-disposition参数以正确处理中文文件名
                if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
                    Map<String, Object> queryParams = new HashMap<>();
                    try {
                        String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8");
                        queryParams.put("response-content-disposition", "attachment; filename*=UTF-8''" + encodedFileName);
                        signatureRequest.setQueryParams(queryParams);
                    } catch (Exception e) {
                        log.warn("编码文件名失败: {}", e.getMessage());
                    }
                }
                
                TemporarySignatureResponse signatureResponse = obsClient.createTemporarySignature(signatureRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(signatureResponse.getSignedUrl())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", System.currentTimeMillis() + expiration * 1000));
                        
            } catch (Exception e) {
                log.error("华为云OBS生成预签名URL失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GENERATE_PRESIGNED_URL_FAILED", "生成预签名URL失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> copyFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                CopyObjectRequest copyRequest = new CopyObjectRequest(request.getBucketName(), sourceObjectKey, 
                        request.getBucketName(), targetObjectKey);
                obsClient.copyObject(copyRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
                        
            } catch (Exception e) {
                log.error("华为云OBS复制文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("COPY_FILE_FAILED", "复制文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
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
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                
                CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
                obsClient.createBucket(createRequest);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("华为云OBS创建存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("CREATE_BUCKET_FAILED", "创建存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                obsClient.deleteBucket(request.getBucketName());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("华为云OBS删除存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_BUCKET_FAILED", "删除存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
                }
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> bucketExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            ObsClient obsClient = null;
            
            try {
                obsClient = createObsClient(request);
                boolean exists = obsClient.headBucket(request.getBucketName());
                
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
                if (obsClient != null) {
                    try {
                        obsClient.close();
                    } catch (IOException e) {
                        log.warn("关闭华为云OBS客户端失败: {}", e.getMessage());
                    }
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