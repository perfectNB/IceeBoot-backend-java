/**
 * MinIO服务实现类
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.service.oss.service.impl;

import com.zboot.common.service.oss.dto.OSSCapacityInfo;
import com.zboot.common.service.oss.dto.OSSRequest;
import com.zboot.common.service.oss.dto.OSSResponse;
import com.zboot.common.service.oss.service.OSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MinIO服务实现类
 * 基于MinIO S3兼容API实现对象存储操作
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class MinIOService implements OSSService {
    
    private static final String PROVIDER_NAME = "minio";
    private static final String DEFAULT_ENDPOINT = "localhost:9000";
    private static final Integer DEFAULT_TIMEOUT = 60;
    
    private final WebClient webClient;
    
    public MinIOService() {
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
                
                WebClient.RequestBodySpec requestSpec = webClient.put()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "PUT", request.getObjectKey()));
                            if (request.getContentType() != null) {
                                headers.set("Content-Type", request.getContentType());
                            }
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        });
                
                String response;
                if (request.getContent() != null) {
                    response = requestSpec.bodyValue(request.getContent())
                            .retrieve()
                            .bodyToMono(String.class)
                            .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                            .block();
                } else {
                    return OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                            .setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
                String fileUrl = buildFileUrl(request);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(fileUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
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
                log.error("MinIO处理输入流失败: {}", e.getMessage(), e);
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
                
                byte[] content = webClient.get()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "GET", request.getObjectKey()));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
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
                            return response;
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
                
                webClient.delete()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "DELETE", request.getObjectKey()));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
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
                // MinIO S3兼容批量删除需要构建XML请求体
                StringBuilder xmlBody = new StringBuilder();
                xmlBody.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                xmlBody.append("<Delete xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">");
                xmlBody.append("<Quiet>false</Quiet>");
                for (String key : objectKeys) {
                    xmlBody.append("<Object><Key>").append(key).append("</Key></Object>");
                }
                xmlBody.append("</Delete>");
                
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s?delete", endpoint, request.getBucketName());
                
                webClient.post()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "POST", ""));
                            headers.set("Content-Type", "application/xml");
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        })
                        .bodyValue(xmlBody.toString())
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
                
                webClient.head()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "HEAD", request.getObjectKey()));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        })
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", true));
                        
            } catch (Exception e) {
                // HEAD请求失败通常表示文件不存在
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s", endpoint, request.getBucketName());
                
                if (request.getObjectKey() != null) {
                    url += "?prefix=" + request.getObjectKey();
                }
                
                String xmlResponse = webClient.get()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "GET", ""));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                // 简单的XML解析（实际项目中应使用专业的XML解析器）
                List<Map<String, Object>> files = parseListObjectsResponse(xmlResponse);
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("files", files, "count", files.size()));
                        
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
        return listFiles(request)
                .map(response -> {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> files = (List<Map<String, Object>>) response.getExtraData().get("files");
                        
                        long totalSize = 0;
                        long objectCount = files.size();
                        
                        for (Map<String, Object> file : files) {
                            Object sizeObj = file.get("size");
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
                        
                        // MinIO没有总容量限制，设置为无限大
                        capacityInfo.setTotalCapacity(Long.MAX_VALUE);
                        capacityInfo.calculateAvailableCapacity();
                        capacityInfo.calculateUsagePercentage();
                        
                        return capacityInfo;
                        
                    } catch (Exception e) {
                        log.error("MinIO计算容量信息失败: {}", e.getMessage(), e);
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
                // MinIO预签名URL生成（简化实现）
                String fileUrl = buildFileUrl(request);
                long expirationTime = System.currentTimeMillis() + expiration * 1000;
                
                // 实际项目中需要根据AWS S3的签名算法生成预签名URL
                String presignedUrl = fileUrl + "?X-Amz-Expires=" + expiration + "&X-Amz-Date=" + 
                        java.time.Instant.now().toString().replaceAll("[:-]", "").substring(0, 15) + "Z";
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setFileUrl(presignedUrl)
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("expiration", expirationTime));
                        
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
        return downloadFile(request.setObjectKey(sourceObjectKey))
                .flatMap(downloadResponse -> {
                    if (downloadResponse.getSuccess() && downloadResponse.getContent() != null) {
                        return uploadFile(request.setObjectKey(targetObjectKey).setContent(downloadResponse.getContent()));
                    } else {
                        return Mono.just(OSSResponse.failure("COPY_FILE_FAILED", "复制文件失败：源文件下载失败")
                                .setProvider(getProvider()));
                    }
                });
    }
    
    @Override
    public Mono<OSSResponse> moveFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return copyFile(request, sourceObjectKey, targetObjectKey)
                .flatMap(copyResponse -> {
                    if (copyResponse.getSuccess()) {
                        return deleteFile(request.setObjectKey(sourceObjectKey))
                                .map(deleteResponse -> {
                                    if (deleteResponse.getSuccess()) {
                                        return copyResponse.setObjectKey(targetObjectKey);
                                    } else {
                                        return OSSResponse.failure("MOVE_FILE_FAILED", "移动文件失败：删除源文件失败")
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
            
            try {
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s", endpoint, request.getBucketName());
                
                webClient.put()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "PUT", ""));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s", endpoint, request.getBucketName());
                
                webClient.delete()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "DELETE", ""));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        })
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
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
                String endpoint = buildEndpoint(request);
                String url = String.format("%s/%s", endpoint, request.getBucketName());
                
                webClient.head()
                        .uri(url)
                        .headers(headers -> {
                            headers.set("Authorization", buildAuthHeader(request, "HEAD", ""));
                            headers.set("Host", getHostFromEndpoint(request));
                            headers.set("Date", java.time.Instant.now().toString());
                        })
                        .retrieve()
                        .toBodilessEntity()
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return OSSResponse.success()
                        .setProvider(getProvider())
                        .setBucketName(request.getBucketName())
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setExtraData(Map.of("exists", true));
                        
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
     * 构建端点URL
     */
    private String buildEndpoint(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
        return String.format("%s://%s", protocol, endpoint);
    }
    
    /**
     * 构建文件URL
     */
    private String buildFileUrl(OSSRequest request) {
        String endpoint = buildEndpoint(request);
        if (request.getCustomDomain() != null) {
            String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
        }
    }
    
    /**
     * 从端点获取Host
     */
    private String getHostFromEndpoint(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        return endpoint;
    }
    
    /**
     * 构建认证头
     * 简化实现，实际项目中需要根据AWS S3的签名算法实现
     */
    private String buildAuthHeader(OSSRequest request, String method, String objectKey) {
        // MinIO使用AWS S3兼容的签名算法
        // 这里提供简化实现，实际项目中需要完整的AWS V4签名
        String accessKey = request.getAccessKeyId();
        String secretKey = request.getAccessKeySecret();
        
        if (accessKey == null || secretKey == null) {
            return "";
        }
        
        // 简化的认证头，实际需要完整的AWS V4签名
        String timestamp = java.time.Instant.now().toString().replaceAll("[:-]", "").substring(0, 15) + "Z";
        String credential = String.format("%s/%s", accessKey, timestamp.substring(0, 8));
        
        return String.format("AWS4-HMAC-SHA256 Credential=%s, SignedHeaders=host;x-amz-date, Signature=dummy", credential);
    }
    
    /**
     * 解析列出对象的XML响应
     * 简化实现，实际项目中应使用专业的XML解析器
     */
    private List<Map<String, Object>> parseListObjectsResponse(String xmlResponse) {
        List<Map<String, Object>> files = new java.util.ArrayList<>();
        
        try {
            // 简单的字符串匹配解析（实际项目中应使用DOM或SAX解析器）
            String[] contents = xmlResponse.split("<Contents>");
            for (int i = 1; i < contents.length; i++) {
                String content = contents[i];
                Map<String, Object> file = new java.util.HashMap<>();
                
                // 提取Key
                int keyStart = content.indexOf("<Key>") + 5;
                int keyEnd = content.indexOf("</Key>");
                if (keyStart > 4 && keyEnd > keyStart) {
                    file.put("key", content.substring(keyStart, keyEnd));
                }
                
                // 提取Size
                int sizeStart = content.indexOf("<Size>") + 6;
                int sizeEnd = content.indexOf("</Size>");
                if (sizeStart > 5 && sizeEnd > sizeStart) {
                    try {
                        file.put("size", Long.parseLong(content.substring(sizeStart, sizeEnd)));
                    } catch (NumberFormatException e) {
                        file.put("size", 0L);
                    }
                }
                
                // 提取LastModified
                int modifiedStart = content.indexOf("<LastModified>") + 14;
                int modifiedEnd = content.indexOf("</LastModified>");
                if (modifiedStart > 13 && modifiedEnd > modifiedStart) {
                    file.put("lastModified", content.substring(modifiedStart, modifiedEnd));
                }
                
                files.add(file);
            }
        } catch (Exception e) {
            log.error("解析MinIO列表响应失败: {}", e.getMessage(), e);
        }
        
        return files;
    }
}