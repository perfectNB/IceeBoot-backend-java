/**
 * 七牛云Kodo服务实现类
 * 使用七牛云官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.service.oss.service.impl;

import com.google.gson.Gson;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.*;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;
import com.iceeboot.common.service.oss.dto.OSSCapacityInfo;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 七牛云Kodo服务实现类
 * 使用七牛云官方SDK实现
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class QiniuKodoService implements OSSService {
    
    private static final String PROVIDER_NAME = "qiniu";
    private static final String DEFAULT_REGION = "z0";
    
    private Auth createAuth(OSSRequest request) {
        return Auth.create(request.getAccessKeyId(), request.getAccessKeySecret());
    }
    
    private Configuration createConfiguration(OSSRequest request) {
        Configuration cfg = new Configuration();
        
        // 设置区域
        String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
        switch (regionName) {
            case "z0":
                cfg.region = Region.region0();
                break;
            case "z1":
                cfg.region = Region.region1();
                break;
            case "z2":
                cfg.region = Region.region2();
                break;
            case "na0":
                cfg.region = Region.regionNa0();
                break;
            case "as0":
                cfg.region = Region.regionAs0();
                break;
            default:
                cfg.region = Region.autoRegion();
                break;
        }
        
        cfg.useHttpsDomains = request.getUseHttps() == null || request.getUseHttps();
        return cfg;
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
                
                Auth auth = createAuth(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(auth, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                String upToken = auth.uploadToken(request.getBucketName());
                
                Configuration cfg = createConfiguration(request);
                UploadManager uploadManager = new UploadManager(cfg);
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
                Response response = uploadManager.put(inputStream, request.getObjectKey(), upToken, null, request.getContentType());
                
                DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("hash", putRet.hash, "key", putRet.key));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo上传文件失败: {}", e.getMessage(), e);
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
                
                Auth auth = createAuth(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(auth, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                String upToken = auth.uploadToken(request.getBucketName());
                
                Configuration cfg = createConfiguration(request);
                UploadManager uploadManager = new UploadManager(cfg);
                
                ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
                Response response = uploadManager.put(inputStream, request.getObjectKey(), upToken, null, request.getContentType());
                
                if (response.isOK()) {
                    DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                    String fileUrl = buildFileUrl(request);
                    
                    return OSSResponse.success()
                            .setProvider(getProvider())
                            .setFileUrl(fileUrl)
                            .setBucketName(request.getBucketName())
                            .setObjectKey(request.getObjectKey())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                } else {
                    return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + response.error)
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
            } catch (Exception e) {
                log.error("七牛云Kodo上传文件失败: {}", e.getMessage(), e);
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
                
                Auth auth = createAuth(request);
                
                // 检查桶是否存在，不存在则创建
                OSSResponse bucketCheckResult = ensureBucketExists(auth, request);
                if (!bucketCheckResult.getSuccess()) {
                    return bucketCheckResult.setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                String upToken = auth.uploadToken(request.getBucketName());
                
                Configuration cfg = createConfiguration(request);
                UploadManager uploadManager = new UploadManager(cfg);
                
                ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
                Response response = uploadManager.put(byteInputStream, request.getObjectKey(), upToken, null, request.getContentType());
                
                if (response.isOK()) {
                    DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
                    String fileUrl = buildFileUrl(request);
                    
                    return OSSResponse.success()
                            .setProvider(getProvider())
                            .setFileUrl(fileUrl)
                            .setBucketName(request.getBucketName())
                            .setObjectKey(request.getObjectKey())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                } else {
                    return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + response.error)
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
            } catch (IOException e) {
                return OSSResponse.failure("INVALID_CONTENT", "读取输入流失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("七牛云Kodo上传文件失败: {}", e.getMessage(), e);
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
                Auth auth = createAuth(request);
                String privateUrl = auth.privateDownloadUrl(buildFileUrl(request));
                
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                // 七牛云SDK没有直接下载到字节数组的方法，需要通过URL下载
                java.net.URL url = new java.net.URL(privateUrl);
                try (InputStream is = url.openStream();
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
                log.error("七牛云Kodo下载文件失败: {}", e.getMessage(), e);
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
                            log.error("七牛云Kodo保存文件到本地失败: {}", e.getMessage(), e);
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                bucketManager.delete(request.getBucketName(), request.getObjectKey());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
            } catch (Exception e) {
                log.error("七牛云Kodo删除文件失败: {}", e.getMessage(), e);
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
                for (String key : objectKeys) {
                    batchOperations.addDeleteOp(request.getBucketName(), key);
                }
                
                Response response = bucketManager.batch(batchOperations);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("deletedCount", objectKeys.size(), "batchResponse", response.bodyString()));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo批量删除文件失败: {}", e.getMessage(), e);
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                FileInfo fileInfo = bucketManager.stat(request.getBucketName(), request.getObjectKey());
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", true));
                        
            } catch (QiniuException e) {
                if (e.response != null && e.response.statusCode == 612) {
                    // 文件不存在
                    return OSSResponse.success()
                            .setProvider(getProvider())
                            .setBucketName(request.getBucketName())
                            .setObjectKey(request.getObjectKey())
                            .setResponseTime(System.currentTimeMillis() - startTime)
                            .setExtraData(Map.of("exists", false));
                } else {
                    return OSSResponse.failure("FILE_EXISTS_CHECK_FAILED", "检查文件是否存在失败: " + e.getMessage())
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                FileInfo fileInfo = bucketManager.stat(request.getBucketName(), request.getObjectKey());
                
                Map<String, Object> info = new HashMap<>();
                info.put("size", fileInfo.fsize);
                info.put("hash", fileInfo.hash);
                info.put("mimeType", fileInfo.mimeType);
                info.put("putTime", fileInfo.putTime);
                info.put("type", fileInfo.type);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(info);
                        
            } catch (Exception e) {
                log.error("七牛云Kodo获取文件信息失败: {}", e.getMessage(), e);
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                String prefix = request.getObjectKey();
                String delimiter = "";
                int limit = 1000;
                
                BucketManager.FileListIterator fileListIterator = bucketManager.createFileListIterator(request.getBucketName(), prefix, limit, delimiter);
                
                List<Map<String, Object>> files = new ArrayList<>();
                while (fileListIterator.hasNext()) {
                    FileInfo[] items = fileListIterator.next();
                    for (FileInfo item : items) {
                        Map<String, Object> file = new HashMap<>();
                        file.put("key", item.key);
                        file.put("size", item.fsize);
                        file.put("hash", item.hash);
                        file.put("mimeType", item.mimeType);
                        file.put("putTime", item.putTime);
                        files.add(file);
                    }
                }
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("files", files));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo列出文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("LIST_FILES_FAILED", "列出文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSCapacityInfo> checkCapacity(OSSRequest request) {
        return Mono.fromCallable(() -> {
            // 七牛云Kodo没有直接的容量查询API，返回默认值
            return new OSSCapacityInfo()
                    .setProvider(getProvider())
                    .setBucketName(request.getBucketName())
                    .setTotalCapacity(-1L) // 表示无限制
                    .setUsedCapacity(0L)
                    .setAvailableCapacity(-1L)
                    .setStatisticsTime(LocalDateTime.now());
        });
    }
    
    @Override
    public Mono<OSSResponse> generatePresignedUrl(OSSRequest request, Long expiration) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                Auth auth = createAuth(request);
                String baseUrl = buildFileUrl(request);
                long deadline = System.currentTimeMillis() / 1000 + expiration;
                String privateUrl = auth.privateDownloadUrlWithDeadline(baseUrl, deadline);
                
                // 添加response-content-disposition参数以正确处理中文文件名
                if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
                    try {
                        String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8");
                        String contentDisposition = "attachment; filename*=UTF-8''" + encodedFileName;
                        String separator = privateUrl.contains("?") ? "&" : "?";
                        privateUrl = privateUrl + separator + "response-content-disposition=" + java.net.URLEncoder.encode(contentDisposition, "UTF-8");
                    } catch (Exception e) {
                        log.warn("编码文件名失败: {}", e.getMessage());
                    }
                }
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(privateUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", deadline * 1000));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo生成预签名URL失败: {}", e.getMessage(), e);
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                bucketManager.copy(request.getBucketName(), sourceObjectKey, request.getBucketName(), targetObjectKey);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo复制文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("COPY_FILE_FAILED", "复制文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> moveFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                bucketManager.move(request.getBucketName(), sourceObjectKey, request.getBucketName(), targetObjectKey);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey, "moved", true));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo移动文件失败: {}", e.getMessage(), e);
                return OSSResponse.failure("MOVE_FILE_FAILED", "移动文件失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> createBucket(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 七牛云Kodo的存储空间需要通过控制台或API创建
                // 这里返回成功，实际项目中可能需要调用七牛云的管理API
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("message", "七牛云存储空间需要通过控制台创建"));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo创建存储空间失败: {}", e.getMessage(), e);
                return OSSResponse.failure("CREATE_BUCKET_FAILED", "创建存储空间失败: " + e.getMessage())
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
                // 七牛云Kodo的存储空间删除需要通过控制台或API
                // 这里返回成功，实际项目中可能需要调用七牛云的管理API
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("message", "七牛云存储空间需要通过控制台删除"));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo删除存储空间失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_BUCKET_FAILED", "删除存储空间失败: " + e.getMessage())
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
                Auth auth = createAuth(request);
                Configuration cfg = createConfiguration(request);
                BucketManager bucketManager = new BucketManager(auth, cfg);
                
                // 通过列举存储空间来检查是否存在
                String[] buckets = bucketManager.buckets();
                boolean exists = Arrays.asList(buckets).contains(request.getBucketName());
                
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
     * @param auth 七牛云认证对象
     * @param request OSS请求
     * @return 操作结果
     */
    private OSSResponse ensureBucketExists(Auth auth, OSSRequest request) {
        try {
            // 七牛云需要通过BucketManager检查桶是否存在
            Configuration cfg = createConfiguration(request);
            BucketManager bucketManager = new BucketManager(auth, cfg);
            
            try {
                // 尝试获取桶信息来检查桶是否存在
                bucketManager.getBucketInfo(request.getBucketName());
                // 如果没有抛出异常，说明桶存在
                return OSSResponse.success().setProvider(getProvider());
            } catch (QiniuException e) {
                // 如果是桶不存在的错误，尝试创建桶
                if (e.code() == 631) { // 631表示桶不存在
                    log.info("桶 {} 不存在，尝试创建", request.getBucketName());
                    
                    try {
                        // 七牛云创建桶需要指定区域
                        String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
                        bucketManager.createBucket(request.getBucketName(), regionName);
                        log.info("成功创建桶: {}", request.getBucketName());
                        return OSSResponse.success().setProvider(getProvider());
                    } catch (QiniuException createException) {
                        String errorMsg = createException.getMessage();
                        log.error("创建桶失败: {}", errorMsg, createException);
                        
                        // 检查是否是桶名已存在的错误
                        if (createException.code() == 614) { // 614表示桶已存在
                            return OSSResponse.failure("BUCKET_ALREADY_EXISTS", 
                                String.format("桶名 '%s' 已被其他用户使用，请更换桶名", request.getBucketName()))
                                .setProvider(getProvider());
                        } else {
                            return OSSResponse.failure("CREATE_BUCKET_FAILED", 
                                String.format("创建桶失败: %s", errorMsg))
                                .setProvider(getProvider());
                        }
                    }
                } else {
                    // 其他错误
                    return OSSResponse.failure("CHECK_BUCKET_FAILED", "检查桶存在性失败: " + e.getMessage())
                            .setProvider(getProvider());
                }
            }
            
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
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
        
        if (request.getCustomDomain() != null) {
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            // 七牛云默认域名格式
            return String.format("%s://%s.qiniudn.com/%s", protocol, request.getBucketName(), request.getObjectKey());
        }
    }
}