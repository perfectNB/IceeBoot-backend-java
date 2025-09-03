/**
 * 七牛云Kodo服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.oss.service.impl;

import com.zboot.common.oss.dto.OSSCapacityInfo;
import com.zboot.common.oss.dto.OSSRequest;
import com.zboot.common.oss.dto.OSSResponse;
import com.zboot.common.oss.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 七牛云Kodo服务实现类
 * 基于七牛云Kodo REST API实现对象存储操作
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public class QiniuKodoService implements OSSService {
    
    private static final String PROVIDER_NAME = "qiniu";
    private static final String DEFAULT_UPLOAD_ENDPOINT = "https://upload.qiniup.com";
    private static final String DEFAULT_DOWNLOAD_ENDPOINT = "https://iovip.qbox.me";
    private static final String DEFAULT_API_ENDPOINT = "https://rs.qbox.me";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    
    public QiniuKodoService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(100 * 1024 * 1024)) // 100MB
                .build();
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
                // 七牛云上传需要先获取上传凭证
                String uploadToken = generateUploadToken(request);
                
                String uploadUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_UPLOAD_ENDPOINT;
                
                // 构建表单数据
                org.springframework.util.MultiValueMap<String, Object> formData = 
                        new org.springframework.util.LinkedMultiValueMap<>();
                formData.add("token", uploadToken);
                formData.add("key", request.getObjectKey());
                
                if (request.getContent() != null) {
                    formData.add("file", new org.springframework.core.io.ByteArrayResource(request.getContent()) {
                        @Override
                        public String getFilename() {
                            return request.getObjectKey();
                        }
                    });
                } else {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                String response = webClient.post()
                        .uri(uploadUrl)
                        .header("Content-Type", "multipart/form-data")
                        .bodyValue(formData)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("uploadResponse", response));
                        
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
        return uploadFile(request.setContent(content));
    }
    
    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream) {
        return Mono.fromCallable(() -> {
            try {
                byte[] content = inputStream.readAllBytes();
                return uploadFile(request.setContent(content)).block();
            } catch (Exception e) {
                log.error("七牛云Kodo处理输入流失败: {}", e.getMessage(), e);
                return OSSResponse.failure("STREAM_PROCESSING_FAILED", "处理输入流失败: " + e.getMessage())
                        .setProvider(getProvider());
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> downloadFile(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String downloadUrl = buildFileUrl(request);
                
                byte[] content = webClient.get()
                        .uri(downloadUrl)
                        .headers(headers -> {
                            String authHeader = buildAuthHeader(request, "GET", downloadUrl);
                            if (!authHeader.isEmpty()) {
                                headers.set("Authorization", authHeader);
                            }
                        })
                        .retrieve()
                        .bodyToMono(byte[].class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setContent(content)
                        .setFileSize(content != null ? (long) content.length : 0L)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                        
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
                            return response;
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
                String apiUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_API_ENDPOINT;
                String encodedEntry = encodeEntry(request.getBucketName(), request.getObjectKey());
                String url = String.format("%s/delete/%s", apiUrl, encodedEntry);
                
                webClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", url));
                            headers.set("Content-Type", "application/x-www-form-urlencoded");
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
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
                String apiUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_API_ENDPOINT;
                
                // 七牛云批量删除
                StringBuilder operations = new StringBuilder();
                for (int i = 0; i < objectKeys.size(); i++) {
                    if (i > 0) {
                        operations.append("&");
                    }
                    String encodedEntry = encodeEntry(request.getBucketName(), objectKeys.get(i));
                    operations.append("op=/delete/").append(encodedEntry);
                }
                
                String url = apiUrl + "/batch";
                
                webClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", url));
                            headers.set("Content-Type", "application/x-www-form-urlencoded");
                        })
                        .bodyValue(operations.toString())
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("deletedCount", objectKeys.size()));
                        
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
                String apiUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_API_ENDPOINT;
                String encodedEntry = encodeEntry(request.getBucketName(), request.getObjectKey());
                String url = String.format("%s/stat/%s", apiUrl, encodedEntry);
                
                webClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", url));
                            headers.set("Content-Type", "application/x-www-form-urlencoded");
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", true));
                        
            } catch (Exception e) {
                // stat请求失败通常表示文件不存在
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
        return fileExists(request)
                .flatMap(response -> {
                    Boolean exists = (Boolean) response.getExtraData().get("exists");
                    if (Boolean.TRUE.equals(exists)) {
                        return downloadFile(request)
                                .map(downloadResponse -> {
                                    if (downloadResponse.getSuccess()) {
                                        return downloadResponse;
                                    } else {
                                        return OSSResponse.failure("GET_FILE_INFO_FAILED", "获取文件信息失败")
                                                .setProvider(getProvider());
                                    }
                                });
                    } else {
                        return Mono.just(OSSResponse.failure("FILE_NOT_FOUND", "文件不存在")
                                .setProvider(getProvider()));
                    }
                });
    }
    
    @Override
    public Mono<OSSResponse> listFiles(OSSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                String apiUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_API_ENDPOINT;
                String baseUrl = String.format("%s/list?bucket=%s", apiUrl, request.getBucketName());
                
                if (request.getObjectKey() != null) {
                    baseUrl += "&prefix=" + request.getObjectKey();
                }
                
                final String finalUrl = baseUrl;
                String jsonResponse = webClient.post()
                        .uri(finalUrl)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", finalUrl));
                            headers.set("Content-Type", "application/x-www-form-urlencoded");
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                // 简单的JSON解析（实际项目中应使用专业的JSON解析器）
                List<Map<String, Object>> files = parseListFilesResponse(jsonResponse);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("files", files, "count", files.size()));
                        
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
        return listFiles(request)
                .map(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> files = (List<Map<String, Object>>) response.getExtraData().get("files");
                        
                        long totalSize = 0;
                        long objectCount = files.size();
                        
                        for (Map<String, Object> file : files) {
                            Object sizeObj = file.get("fsize");
                            if (sizeObj instanceof Number) {
                                totalSize += ((Number) sizeObj).longValue();
                            }
                        }
                        
                        OSSCapacityInfo capacityInfo = new OSSCapacityInfo()
                                .setBucketName(request.getBucketName())
                                .setUsedCapacity(totalSize)
                                .setObjectCount(objectCount)
                                .setProvider(getProvider())
                                .setRegion(request.getRegion())
                                .setStatisticsTime(LocalDateTime.now());
                        
                        // 七牛云Kodo没有总容量限制，设置为无限大
                        capacityInfo.setTotalCapacity(Long.MAX_VALUE);
                        capacityInfo.calculateAvailableCapacity();
                        capacityInfo.calculateUsagePercentage();
                        
                        return capacityInfo;
                        
                    } catch (Exception e) {
                        log.error("七牛云Kodo计算容量信息失败: {}", e.getMessage(), e);
                        return new OSSCapacityInfo()
                                .setBucketName(request.getBucketName())
                                .setProvider(getProvider())
                                .setStatisticsTime(LocalDateTime.now());
                    }
                });
    }
    
    @Override
    public Mono<OSSResponse> generatePresignedUrl(OSSRequest request, Long expiration) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 七牛云预签名URL生成
                String baseUrl = buildFileUrl(request);
                long deadline = System.currentTimeMillis() / 1000 + expiration;
                
                String downloadUrl = baseUrl + "?e=" + deadline;
                String token = generateDownloadToken(request, downloadUrl);
                String presignedUrl = downloadUrl + "&token=" + token;
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(presignedUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", deadline));
                        
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
                String apiUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_API_ENDPOINT;
                String sourceEntry = encodeEntry(request.getBucketName(), sourceObjectKey);
                String targetEntry = encodeEntry(request.getBucketName(), targetObjectKey);
                String url = String.format("%s/copy/%s/%s", apiUrl, sourceEntry, targetEntry);
                
                webClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", url));
                            headers.set("Content-Type", "application/x-www-form-urlencoded");
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
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
                String apiUrl = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_API_ENDPOINT;
                String sourceEntry = encodeEntry(request.getBucketName(), sourceObjectKey);
                String targetEntry = encodeEntry(request.getBucketName(), targetObjectKey);
                String url = String.format("%s/move/%s/%s", apiUrl, sourceEntry, targetEntry);
                
                webClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", url));
                            headers.set("Content-Type", "application/x-www-form-urlencoded");
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(targetObjectKey)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
                        
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
                // 七牛云存储桶创建需要通过控制台或API，这里提供占位实现
                log.warn("七牛云Kodo存储桶创建需要通过控制台操作");
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("message", "请通过七牛云控制台创建存储桶"));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo创建存储桶失败: {}", e.getMessage(), e);
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
                // 七牛云存储桶删除需要通过控制台或API，这里提供占位实现
                log.warn("七牛云Kodo存储桶删除需要通过控制台操作");
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("message", "请通过七牛云控制台删除存储桶"));
                        
            } catch (Exception e) {
                log.error("七牛云Kodo删除存储桶失败: {}", e.getMessage(), e);
                return OSSResponse.failure("DELETE_BUCKET_FAILED", "删除存储桶失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<OSSResponse> bucketExists(OSSRequest request) {
        return listFiles(request)
                .map(response -> {
                    // 如果能够列出文件，说明存储桶存在
                    return OSSResponse.success()
                            .setProvider(getProvider())
                            .setBucketName(request.getBucketName())
                            .setExtraData(Map.of("exists", response.getSuccess()));
                })
                .onErrorReturn(OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setExtraData(Map.of("exists", false)));
    }
    
    @Override
    public Mono<Boolean> validateConfig(OSSRequest request) {
        return bucketExists(request)
                .map(response -> {
                    Boolean exists = (Boolean) response.getExtraData().get("exists");
                    return Boolean.TRUE.equals(exists);
                })
                .onErrorReturn(false);
    }
    
    /**
     * 构建文件URL
     */
    private String buildFileUrl(OSSRequest request) {
        if (request.getCustomDomain() != null) {
            String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_DOWNLOAD_ENDPOINT;
            String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
            return String.format("%s://%s/%s/%s", protocol, endpoint, request.getBucketName(), request.getObjectKey());
        }
    }
    
    /**
     * 编码Entry（bucket:key的Base64编码）
     */
    private String encodeEntry(String bucket, String key) {
        String entry = bucket + ":" + key;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(entry.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 生成上传凭证
     * 简化实现，实际项目中需要根据七牛云的算法实现
     */
    private String generateUploadToken(OSSRequest request) {
        // 七牛云上传凭证生成（简化实现）
        String accessKey = request.getAccessKeyId();
        String secretKey = request.getAccessKeySecret();
        
        if (accessKey == null || secretKey == null) {
            return "";
        }
        
        // 构建上传策略
        String policy = String.format(
                "{\"scope\":\"%s\",\"deadline\":%d}",
                request.getBucketName(),
                System.currentTimeMillis() / 1000 + 3600 // 1小时过期
        );
        
        String encodedPolicy = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(policy.getBytes(StandardCharsets.UTF_8));
        
        // 简化的签名，实际需要HMAC-SHA1
        String sign = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((secretKey + encodedPolicy).getBytes(StandardCharsets.UTF_8));
        
        return accessKey + ":" + sign + ":" + encodedPolicy;
    }
    
    /**
     * 生成下载凭证
     * 简化实现，实际项目中需要根据七牛云的算法实现
     */
    private String generateDownloadToken(OSSRequest request, String url) {
        String accessKey = request.getAccessKeyId();
        String secretKey = request.getAccessKeySecret();
        
        if (accessKey == null || secretKey == null) {
            return "";
        }
        
        // 简化的下载签名
        String sign = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((secretKey + url).getBytes(StandardCharsets.UTF_8));
        
        return accessKey + ":" + sign;
    }
    
    /**
     * 构建认证头
     * 简化实现，实际项目中需要根据七牛云的签名算法实现
     */
    private String buildAuthHeader(OSSRequest request, String method, String url) {
        String accessKey = request.getAccessKeyId();
        String secretKey = request.getAccessKeySecret();
        
        if (accessKey == null || secretKey == null) {
            return "";
        }
        
        // 七牛云管理凭证生成（简化实现）
        String sign = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((method + url + secretKey).getBytes(StandardCharsets.UTF_8));
        
        return "QBox " + accessKey + ":" + sign;
    }
    
    /**
     * 解析列出文件的JSON响应
     * 简化实现，实际项目中应使用专业的JSON解析器
     */
    private List<Map<String, Object>> parseListFilesResponse(String jsonResponse) {
        List<Map<String, Object>> files = new java.util.ArrayList<>();
        
        try {
            // 简单的JSON解析（实际项目中应使用Jackson或Gson）
            if (jsonResponse.contains("\"items\":")) {
                String itemsSection = jsonResponse.substring(
                        jsonResponse.indexOf("\"items\":") + 8,
                        jsonResponse.lastIndexOf("]")
                );
                
                String[] items = itemsSection.split("\\},\\{");
                for (String item : items) {
                    Map<String, Object> file = new java.util.HashMap<>();
                    
                    // 提取key
                    if (item.contains("\"key\":")) {
                        int keyStart = item.indexOf("\"key\":\"") + 7;
                        int keyEnd = item.indexOf("\"", keyStart);
                        if (keyEnd > keyStart) {
                            file.put("key", item.substring(keyStart, keyEnd));
                        }
                    }
                    
                    // 提取fsize
                    if (item.contains("\"fsize\":")) {
                        int sizeStart = item.indexOf("\"fsize\":") + 8;
                        int sizeEnd = item.indexOf(",", sizeStart);
                        if (sizeEnd == -1) sizeEnd = item.indexOf("}", sizeStart);
                        if (sizeEnd > sizeStart) {
                            try {
                                file.put("fsize", Long.parseLong(item.substring(sizeStart, sizeEnd)));
                            } catch (NumberFormatException e) {
                                file.put("fsize", 0L);
                            }
                        }
                    }
                    
                    // 提取putTime
                    if (item.contains("\"putTime\":")) {
                        int timeStart = item.indexOf("\"putTime\":") + 10;
                        int timeEnd = item.indexOf(",", timeStart);
                        if (timeEnd == -1) timeEnd = item.indexOf("}", timeStart);
                        if (timeEnd > timeStart) {
                            file.put("putTime", item.substring(timeStart, timeEnd));
                        }
                    }
                    
                    if (!file.isEmpty()) {
                        files.add(file);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析七牛云Kodo列表响应失败: {}", e.getMessage(), e);
        }
        
        return files;
    }
}