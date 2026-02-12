package com.iceeboot.common.service.oss.service;

import com.iceeboot.common.service.oss.dto.OSSCapacityInfo;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * OSS服务抽象基类
 * 封装了所有OSS服务实现的公共模板逻辑：Mono包装、计时、客户端生命周期管理、异常处理
 * 子类只需实现具体的SDK调用逻辑
 *
 * @param <C> 云厂商SDK客户端类型
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public abstract class AbstractOSSService<C> implements OSSService {

    // ==================== 功能接口 ====================

    /**
     * 客户端操作功能接口
     * @param <C> 客户端类型
     */
    @FunctionalInterface
    protected interface ClientOperation<C> {
        OSSResponse execute(C client) throws Exception;
    }

    // ==================== 抽象方法：子类必须实现 ====================

    /**
     * 获取厂商标识名称
     * @return 厂商标识（如 aliyun、tencent、huawei、minio、qiniu）
     */
    @Override
    public abstract String getProvider();

    /**
     * 获取厂商显示名称（用于日志）
     * @return 厂商显示名称（如 阿里云OSS、腾讯云COS）
     */
    public abstract String getProviderDisplayName();

    /**
     * 获取默认端点
     * @return 默认端点地址
     */
    public abstract String getDefaultEndpoint();

    /**
     * 创建SDK客户端
     * @param request OSS请求
     * @return SDK客户端实例
     */
    protected abstract C createClient(OSSRequest request);

    /**
     * 关闭SDK客户端
     * @param client SDK客户端实例
     */
    protected abstract void shutdownClient(C client);

    /**
     * 执行上传文件（使用request中的content）
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doUploadFile(C client, OSSRequest request) throws Exception;

    /**
     * 执行上传文件（使用字节数组）
     * @param client SDK客户端
     * @param request OSS请求
     * @param content 文件内容
     * @return 操作结果
     */
    protected abstract OSSResponse doUploadFile(C client, OSSRequest request, byte[] content) throws Exception;

    /**
     * 执行上传文件（使用输入流）
     * @param client SDK客户端
     * @param request OSS请求
     * @param inputStream 文件输入流
     * @return 操作结果
     */
    protected abstract OSSResponse doUploadFileFromStream(C client, OSSRequest request, InputStream inputStream) throws Exception;

    /**
     * 执行下载文件
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doDownloadFile(C client, OSSRequest request) throws Exception;

    /**
     * 执行下载文件到本地
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doDownloadFileToLocal(C client, OSSRequest request) throws Exception;

    /**
     * 执行删除文件
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doDeleteFile(C client, OSSRequest request) throws Exception;

    /**
     * 执行批量删除文件
     * @param client SDK客户端
     * @param request OSS请求
     * @param objectKeys 对象键列表
     * @return 操作结果
     */
    protected abstract OSSResponse doDeleteFiles(C client, OSSRequest request, List<String> objectKeys) throws Exception;

    /**
     * 执行检查文件是否存在
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doFileExists(C client, OSSRequest request) throws Exception;

    /**
     * 执行获取文件信息
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doGetFileInfo(C client, OSSRequest request) throws Exception;

    /**
     * 执行列出文件
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doListFiles(C client, OSSRequest request) throws Exception;

    /**
     * 执行生成预签名URL
     * @param client SDK客户端
     * @param request OSS请求
     * @param expiration 过期时间（秒）
     * @return 操作结果
     */
    protected abstract OSSResponse doGeneratePresignedUrl(C client, OSSRequest request, Long expiration) throws Exception;

    /**
     * 执行复制文件
     * @param client SDK客户端
     * @param request OSS请求
     * @param sourceObjectKey 源对象键
     * @param targetObjectKey 目标对象键
     * @return 操作结果
     */
    protected abstract OSSResponse doCopyFile(C client, OSSRequest request, String sourceObjectKey, String targetObjectKey) throws Exception;

    /**
     * 执行创建存储桶
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doCreateBucket(C client, OSSRequest request) throws Exception;

    /**
     * 执行删除存储桶
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doDeleteBucket(C client, OSSRequest request) throws Exception;

    /**
     * 执行检查存储桶是否存在
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doBucketExists(C client, OSSRequest request) throws Exception;

    /**
     * 确保存储桶存在，不存在则创建
     * @param client SDK客户端
     * @param request OSS请求
     * @return 操作结果
     */
    protected abstract OSSResponse doEnsureBucketExists(C client, OSSRequest request) throws Exception;

    // ==================== 核心模板方法 ====================

    /**
     * 使用客户端执行操作的模板方法
     * 封装了Mono包装、计时、客户端生命周期管理和异常处理
     *
     * @param request   OSS请求
     * @param operation 操作描述（用于日志）
     * @param errorCode 错误代码
     * @param action    具体操作逻辑
     * @return 操作结果
     */
    protected Mono<OSSResponse> executeWithClient(OSSRequest request, String operation, String errorCode, ClientOperation<C> action) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            C client = null;
            try {
                client = createClient(request);
                OSSResponse response = action.execute(client);
                return response.setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("{}{}失败: {}", getProviderDisplayName(), operation, e.getMessage(), e);
                return OSSResponse.failure(errorCode, operation + "失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (client != null) {
                    try {
                        shutdownClient(client);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    // ==================== OSSService接口实现 ====================

    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request) {
        if (request.getContent() == null) {
            return Mono.just(OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                    .setProvider(getProvider()));
        }
        return executeWithClient(request, "上传文件", "UPLOAD_FAILED", client -> {
            OSSResponse bucketCheck = doEnsureBucketExists(client, request);
            if (!bucketCheck.getSuccess()) {
                return bucketCheck;
            }
            return doUploadFile(client, request);
        });
    }

    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, byte[] content) {
        if (content == null) {
            return Mono.just(OSSResponse.failure("INVALID_CONTENT", "文件内容不能为空")
                    .setProvider(getProvider()));
        }
        return executeWithClient(request, "上传文件", "UPLOAD_FAILED", client -> {
            OSSResponse bucketCheck = doEnsureBucketExists(client, request);
            if (!bucketCheck.getSuccess()) {
                return bucketCheck;
            }
            return doUploadFile(client, request, content);
        });
    }

    @Override
    public Mono<OSSResponse> uploadFile(OSSRequest request, InputStream inputStream) {
        if (inputStream == null) {
            return Mono.just(OSSResponse.failure("INVALID_CONTENT", "输入流不能为空")
                    .setProvider(getProvider()));
        }
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            C client = null;
            try {
                byte[] content = inputStream.readAllBytes();
                client = createClient(request);
                OSSResponse bucketCheck = doEnsureBucketExists(client, request);
                if (!bucketCheck.getSuccess()) {
                    return bucketCheck.setProvider(getProvider())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                OSSResponse response = doUploadFileFromStream(client, request, inputStream);
                return response.setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (IOException e) {
                return OSSResponse.failure("INVALID_CONTENT", "读取输入流失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("{}上传文件失败: {}", getProviderDisplayName(), e.getMessage(), e);
                return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            } finally {
                if (client != null) {
                    try {
                        shutdownClient(client);
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    @Override
    public Mono<OSSResponse> downloadFile(OSSRequest request) {
        return executeWithClient(request, "下载文件", "DOWNLOAD_FAILED",
                client -> doDownloadFile(client, request));
    }

    @Override
    public Mono<OSSResponse> downloadFileToLocal(OSSRequest request) {
        return executeWithClient(request, "下载文件到本地", "DOWNLOAD_TO_LOCAL_FAILED",
                client -> doDownloadFileToLocal(client, request));
    }

    @Override
    public Mono<OSSResponse> deleteFile(OSSRequest request) {
        return executeWithClient(request, "删除文件", "DELETE_FAILED",
                client -> doDeleteFile(client, request));
    }

    @Override
    public Mono<OSSResponse> deleteFiles(OSSRequest request, List<String> objectKeys) {
        return executeWithClient(request, "批量删除文件", "BATCH_DELETE_FAILED",
                client -> doDeleteFiles(client, request, objectKeys));
    }

    @Override
    public Mono<OSSResponse> fileExists(OSSRequest request) {
        return executeWithClient(request, "检查文件存在", "CHECK_FILE_EXISTS_FAILED",
                client -> doFileExists(client, request));
    }

    @Override
    public Mono<OSSResponse> getFileInfo(OSSRequest request) {
        return executeWithClient(request, "获取文件信息", "GET_FILE_INFO_FAILED",
                client -> doGetFileInfo(client, request));
    }

    @Override
    public Mono<OSSResponse> listFiles(OSSRequest request) {
        return executeWithClient(request, "列出文件", "LIST_FILES_FAILED",
                client -> doListFiles(client, request));
    }

    @Override
    public Mono<OSSCapacityInfo> checkCapacity(OSSRequest request) {
        return Mono.fromCallable(() -> new OSSCapacityInfo()
                .setProvider(getProvider())
                .setBucketName(request.getBucketName())
                .setTotalCapacity(-1L)
                .setUsedCapacity(0L)
                .setAvailableCapacity(-1L)
                .setStatisticsTime(LocalDateTime.now()));
    }

    @Override
    public Mono<OSSResponse> generatePresignedUrl(OSSRequest request, Long expiration) {
        return executeWithClient(request, "生成预签名URL", "GENERATE_PRESIGNED_URL_FAILED",
                client -> doGeneratePresignedUrl(client, request, expiration));
    }

    @Override
    public Mono<OSSResponse> copyFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return executeWithClient(request, "复制文件", "COPY_FILE_FAILED",
                client -> doCopyFile(client, request, sourceObjectKey, targetObjectKey));
    }

    @Override
    public Mono<OSSResponse> moveFile(OSSRequest request, String sourceObjectKey, String targetObjectKey) {
        return copyFile(request, sourceObjectKey, targetObjectKey)
                .flatMap(copyResponse -> {
                    if (copyResponse.getSuccess()) {
                        OSSRequest deleteRequest = new OSSRequest()
                                .setBucketName(request.getBucketName())
                                .setObjectKey(sourceObjectKey)
                                .setAccessKeyId(request.getAccessKeyId())
                                .setAccessKeySecret(request.getAccessKeySecret())
                                .setEndpoint(request.getEndpoint());
                        return deleteFile(deleteRequest)
                                .map(deleteResponse -> {
                                    if (deleteResponse.getSuccess()) {
                                        return copyResponse.setExtraData(Map.of(
                                                "sourceKey", sourceObjectKey,
                                                "targetKey", targetObjectKey,
                                                "moved", true));
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
        return executeWithClient(request, "创建存储桶", "CREATE_BUCKET_FAILED",
                client -> doCreateBucket(client, request));
    }

    @Override
    public Mono<OSSResponse> deleteBucket(OSSRequest request) {
        return executeWithClient(request, "删除存储桶", "DELETE_BUCKET_FAILED",
                client -> doDeleteBucket(client, request));
    }

    @Override
    public Mono<OSSResponse> bucketExists(OSSRequest request) {
        return executeWithClient(request, "检查存储桶存在", "CHECK_BUCKET_FAILED",
                client -> doBucketExists(client, request));
    }

    @Override
    public Mono<Boolean> validateConfig(OSSRequest request) {
        return bucketExists(request)
                .map(OSSResponse::getSuccess)
                .onErrorReturn(false);
    }

    // ==================== 公共辅助方法 ====================

    /**
     * 构建文件URL
     * @param request OSS请求
     * @return 文件访问URL
     */
    protected String buildFileUrl(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";

        if (request.getCustomDomain() != null) {
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s://%s.%s/%s", protocol, request.getBucketName(), endpoint, request.getObjectKey());
        }
    }
}
