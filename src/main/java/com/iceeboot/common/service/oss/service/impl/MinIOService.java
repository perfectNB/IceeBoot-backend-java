/**
 * MinIO服务实现类
 * 使用MinIO官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.oss.service.impl;

import com.iceeboot.common.service.oss.dto.OSSCapacityInfo;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.OSSService;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import io.minio.messages.DeleteObject;
import io.minio.messages.DeleteError;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * MinIO服务实现类
 * 使用MinIO官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class MinIOService implements OSSService {
    
    private static final String PROVIDER_NAME = "minio";
    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";
    
    private MinioClient createMinioClient(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(request.getAccessKeyId(), request.getAccessKeySecret());
        
        if (request.getRegion() != null) {
            builder.region(request.getRegion());
        }
        
        return builder.build();
    }
    
    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                if (request.getContent() == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                MinioClient minioClient = createMinioClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(minioClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
                
                PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .stream(inputStream, request.getContent().length, -1);
                
                if (request.getContentType() != null) {
                    putObjectBuilder.contentType(request.getContentType());
                }
                
                ObjectWriteResponse response = minioClient.putObject(putObjectBuilder.build());
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("etag", response.etag(), "versionId", response.versionId() != null ? response.versionId() : ""));
                        
            } catch (Exception e) {
                log.error("MinIO上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, byte[] content) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                if (content == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                MinioClient minioClient = createMinioClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(minioClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
                
                PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .stream(inputStream, content.length, -1);
                
                if (request.getContentType() != null) {
                    putObjectBuilder.contentType(request.getContentType());
                }
                
                ObjectWriteResponse response = minioClient.putObject(putObjectBuilder.build());
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("etag", response.etag(), "versionId", response.versionId() != null ? response.versionId() : ""));
                        
            } catch (Exception e) {
                log.error("MinIO上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                if (inputStream == null) {
                    return OSSResponse.failure("INVALID_CONTENT", "输入流不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                byte[] content = inputStream.readAllBytes();
                
                MinioClient minioClient = createMinioClient(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(minioClient, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
                
                PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .stream(byteInputStream, content.length, -1);
                
                if (request.getContentType() != null) {
                    putObjectBuilder.contentType(request.getContentType());
                }
                
                ObjectWriteResponse response = minioClient.putObject(putObjectBuilder.build());
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("etag", response.etag(), "versionId", response.versionId() != null ? response.versionId() : ""));
                        
            } catch (IOException e) {
                return OSSResponse.failure("INVALID_CONTENT", "读取输入流失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("MinIO上传文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .build();
                
                try (InputStream is = minioClient.getObject(getObjectArgs);
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    
                    byte[] content = baos.toByteArray();
                    
                    return OSSResponse.success()
                            .setProvider(getProvider())
                            .setContent(content)
                            .setBucketName(request.getBucketName())
                            .setObjectKey(request.getObjectKey())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                        
            } catch (Exception e) {
                log.error("MinIO下载文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DOWNLOAD_FAILED", "下载失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFileToLocal(OSSRequest request) {
        return downloadFile(request)
                .map(response -> {
                    if (response.getSuccess() && response.getContent() != null) {
                        try {
                            java.nio.file.Files.write(
                                    java.nio.file.Paths.get(request.getLocalFilePath()),
                                    response.getContent()
                            );
                            return response.setExtraData(Map.of("localPath", request.getLocalFilePath()));
                        } catch (Exception e) {
                            log.error("MinIO保存文件到本地失败: {}", e.getMessage(), e);
                            return OSSResponse.failure("SAVE_TO_LOCAL_FAILED", "保存到本地失败: " + e.getMessage())
                                    .setProvider(getProvider());
                        }
                    }
                    return response;
                });
    }
    
    @Override
    public Mono<OSSResponse> deleteFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .build();
                
                minioClient.removeObject(removeObjectArgs);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("MinIO删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_FAILED", "删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteFiles(OSSRequest request, List<String> objectKeys) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                List<DeleteObject> objects = new ArrayList<>();
                for (String key : objectKeys) {
                    objects.add(new DeleteObject(key));
                }
                
                RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                        .bucket(request.getBucketName())
                        .objects(objects)
                        .build();
                
                Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
                
                int deletedCount = 0;
                List<String> errors = new ArrayList<>();
                
                for (Result<DeleteError> result : results) {
                    try {
                        DeleteError error = result.get();
                        if (error != null) {
                            errors.add(error.objectName() + ": " + error.message());
                        } else {
                            deletedCount++;
                        }
                    } catch (Exception e) {
                        deletedCount++; // 没有错误表示删除成功
                    }
                }
                
                Map<String, Object> extraData = new HashMap<>();
                extraData.put("deletedCount", deletedCount);
                extraData.put("totalCount", objectKeys.size());
                if (!errors.isEmpty()) {
                    extraData.put("errors", errors);
                }
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(extraData);
                        
            } catch (Exception e) {
                log.error("MinIO批量删除文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("BATCH_DELETE_FAILED", "批量删除失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> fileExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .build();
                
                StatObjectResponse stat = minioClient.statObject(statObjectArgs);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", true));
                        
            } catch (Exception e) {
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", false));
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> getFileInfo(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .build();
                
                StatObjectResponse stat = minioClient.statObject(statObjectArgs);
                
                Map<String, Object> info = new HashMap<>();
                info.put("size", stat.size());
                info.put("etag", stat.etag());
                info.put("contentType", stat.contentType());
                info.put("lastModified", stat.lastModified());
                info.put("versionId", stat.versionId());
                if (stat.userMetadata() != null) {
                    info.put("userMetadata", stat.userMetadata());
                }
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(info);
                        
            } catch (Exception e) {
                log.error("MinIO获取文件信息失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GET_FILE_INFO_FAILED", "获取文件信息失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> listFiles(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                ListObjectsArgs.Builder listObjectsBuilder = ListObjectsArgs.builder()
                        .bucket(request.getBucketName());
                
                if (request.getObjectKey() != null) {
                    listObjectsBuilder.prefix(request.getObjectKey());
                }
                
                Iterable<Result<Item>> results = minioClient.listObjects(listObjectsBuilder.build());
                
                List<Map<String, Object>> files = new ArrayList<>();
                for (Result<Item> result : results) {
                    Item item = result.get();
                    Map<String, Object> file = new HashMap<>();
                    file.put("key", item.objectName());
                    file.put("size", item.size());
                    file.put("etag", item.etag());
                    file.put("lastModified", item.lastModified());
                    file.put("isDir", item.isDir());
                    file.put("storageClass", item.storageClass());
                    files.add(file);
                }
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("files", files));
                        
            } catch (Exception e) {
                log.error("MinIO列出文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("LIST_FILES_FAILED", "列出文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSCapacityInfo> checkCapacity(OSSRequest request) {
        return Mono.fromCallable(() -> {
             // MinIO没有直接的容量查询API，返回默认值
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
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                GetPresignedObjectUrlArgs.Builder argsBuilder = GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(request.getBucketName())
                        .object(request.getObjectKey())
                        .expiry(expiration.intValue(), TimeUnit.SECONDS);
                
                // 添加response-content-disposition参数以正确处理中文文件名
                if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
                    try {
                        String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8");
                        Map<String, String> extraQueryParams = new HashMap<>();
                        extraQueryParams.put("response-content-disposition", "attachment; filename*=UTF-8''" + encodedFileName);
                        argsBuilder.extraQueryParams(extraQueryParams);
                    } catch (Exception e) {
                        log.warn("编码文件名失败: {}", e.getMessage());
                    }
                }
                
                GetPresignedObjectUrlArgs presignedObjectUrlArgs = argsBuilder.build();
                
                String presignedUrl = minioClient.getPresignedObjectUrl(presignedObjectUrlArgs);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(presignedUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", System.currentTimeMillis() + expiration * 1000));
                        
            } catch (Exception e) {
                log.error("MinIO生成预签名URL失败: {}", e.getMessage(), e);
                return OSSResponse.failure("GENERATE_PRESIGNED_URL_FAILED", "生成预签名URL失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> copyFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                CopySource copySource = CopySource.builder()
                        .bucket(request.getBucketName())
                        .object(sourceObjectKey)
                        .build();
                
                CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                        .bucket(request.getBucketName())
                        .object(targetObjectKey)
                        .source(copySource)
                        .build();
                
                ObjectWriteResponse response = minioClient.copyObject(copyObjectArgs);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey, "etag", response.etag()));
                        
            } catch (Exception e) {
                log.error("MinIO复制文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("COPY_FILE_FAILED", "复制文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> moveFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return copyFile(request, sourceObjectKey, targetObjectKey)
                .flatMap(copyResponse -> {
                    if (copyResponse.getSuccess()) {
                        // 复制成功后删除源文件
                        OSSRequest deleteRequest = new OSSRequest()
                                .setBucketName(request.getBucketName())
                                .setObjectKey(sourceObjectKey)
                                .setAccessKeyId(request.getAccessKeyId())
                                .setAccessKeySecret(request.getAccessKeySecret())
                                .setEndpoint(request.getEndpoint())
                                .setRegion(request.getRegion());
                        
                        return deleteFile(deleteRequest)
                                .map(deleteResponse -> {
                                    if (deleteResponse.getSuccess()) {
                                        return copyResponse.setExtraData(Map.of(
                                                "sourceKey", sourceObjectKey,
                                                "targetKey", targetObjectKey,
                                                "moved", true
                                        ));
                                    } else {
                                        return OSSResponse.failure("MOVE_FILE_FAILED", "移动文件失败: 复制成功但删除源文件失败")
                                                .setProvider(getProvider());
                                    }
                                });
                    } else {
                        return Mono.just(OSSResponse.failure("MOVE_FILE_FAILED", "移动文件失败: 复制文件失败")
                                .setProvider(getProvider()));
                    }
                });
    }
    
    @Override
    public Mono<OSSResponse> createBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                MakeBucketArgs makeBucketArgs = MakeBucketArgs.builder()
                        .bucket(request.getBucketName())
                        .build();
                
                if (request.getRegion() != null) {
                    makeBucketArgs = MakeBucketArgs.builder()
                            .bucket(request.getBucketName())
                            .region(request.getRegion())
                            .build();
                }
                
                minioClient.makeBucket(makeBucketArgs);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("MinIO创建存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("CREATE_BUCKET_FAILED", "创建存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> deleteBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                RemoveBucketArgs removeBucketArgs = RemoveBucketArgs.builder()
                        .bucket(request.getBucketName())
                        .build();
                
                minioClient.removeBucket(removeBucketArgs);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("MinIO删除存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_BUCKET_FAILED", "删除存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> bucketExists(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                MinioClient minioClient = createMinioClient(request);
                
                BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                        .bucket(request.getBucketName())
                        .build();
                
                boolean exists = minioClient.bucketExists(bucketExistsArgs);
                
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
     * @param minioClient MinIO客户端
     * @param request OSS请求
     * @return 操作结果
     */
    private OSSResponse ensureBucketExists(MinioClient minioClient, OSSRequest request) {
        try {
            // 检查桶是否存在
            boolean bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(request.getBucketName())
                    .build()
            );
            
            if (!bucketExists) {
                log.info("桶 {} 不存在，尝试创建", request.getBucketName());
                
                // 尝试创建桶
                try {
                    MakeBucketArgs.Builder makeBucketBuilder = MakeBucketArgs.builder()
                        .bucket(request.getBucketName());
                    
                    if (request.getRegion() != null) {
                        makeBucketBuilder.region(request.getRegion());
                    }
                    
                    minioClient.makeBucket(makeBucketBuilder.build());
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
        
        if (request.getCustomDomain() != null) {
            String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
        }
    }
}