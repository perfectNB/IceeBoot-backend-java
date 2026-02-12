package com.iceeboot.common.service.oss.service.impl;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.*;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.AbstractOSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 阿里云OSS服务实现类
 * 使用阿里云官方SDK实现
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class AliyunOSSService extends AbstractOSSService<OSS> {

    private static final String PROVIDER_NAME = "aliyun";
    private static final String DEFAULT_ENDPOINT = "oss-cn-hangzhou.aliyuncs.com";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "阿里云OSS";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected OSS createClient(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        return new OSSClientBuilder().build(endpoint, request.getAccessKeyId(), request.getAccessKeySecret());
    }

    @Override
    protected void shutdownClient(OSS client) {
        client.shutdown();
    }

    @Override
    protected OSSResponse doUploadFile(OSS client, OSSRequest request) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(request.getContent().length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
        PutObjectResult result = client.putObject(request.getBucketName(), request.getObjectKey(), inputStream, metadata);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setEtag(result.getETag())
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doUploadFile(OSS client, OSSRequest request, byte[] content) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        PutObjectResult result = client.putObject(request.getBucketName(), request.getObjectKey(), inputStream, metadata);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setEtag(result.getETag())
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doUploadFileFromStream(OSS client, OSSRequest request, InputStream inputStream) throws Exception {
        byte[] content = inputStream.readAllBytes();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
        PutObjectResult result = client.putObject(request.getBucketName(), request.getObjectKey(), byteInputStream, metadata);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setEtag(result.getETag())
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDownloadFile(OSS client, OSSRequest request) throws Exception {
        OSSObject ossObject = client.getObject(request.getBucketName(), request.getObjectKey());
        byte[] content = ossObject.getObjectContent().readAllBytes();

        return OSSResponse.success()
                .setContent(content)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDownloadFileToLocal(OSS client, OSSRequest request) throws Exception {
        GetObjectRequest getObjectRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
        client.getObject(getObjectRequest, new File(request.getLocalFilePath()));

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("localPath", request.getLocalFilePath()));
    }

    @Override
    protected OSSResponse doDeleteFile(OSS client, OSSRequest request) throws Exception {
        client.deleteObject(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDeleteFiles(OSS client, OSSRequest request, List<String> objectKeys) throws Exception {
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(request.getBucketName());
        deleteRequest.setKeys(objectKeys);

        DeleteObjectsResult result = client.deleteObjects(deleteRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("deletedCount", result.getDeletedObjects().size()));
    }

    @Override
    protected OSSResponse doFileExists(OSS client, OSSRequest request) throws Exception {
        boolean exists = client.doesObjectExist(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doGetFileInfo(OSS client, OSSRequest request) throws Exception {
        ObjectMetadata metadata = client.getObjectMetadata(request.getBucketName(), request.getObjectKey());

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("size", metadata.getContentLength());
        fileInfo.put("contentType", metadata.getContentType());
        fileInfo.put("lastModified", metadata.getLastModified());
        fileInfo.put("etag", metadata.getETag());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(fileInfo);
    }

    @Override
    protected OSSResponse doListFiles(OSS client, OSSRequest request) throws Exception {
        ListObjectsRequest listRequest = new ListObjectsRequest(request.getBucketName());
        if (request.getObjectKey() != null) {
            listRequest.setPrefix(request.getObjectKey());
        }

        ObjectListing objectListing = client.listObjects(listRequest);

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
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("files", files));
    }

    @Override
    protected OSSResponse doGeneratePresignedUrl(OSS client, OSSRequest request, Long expiration) throws Exception {
        Date expirationDate = new Date(System.currentTimeMillis() + expiration * 1000);

        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                request.getBucketName(),
                request.getObjectKey(),
                HttpMethod.GET
        );
        generatePresignedUrlRequest.setExpiration(expirationDate);

        if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
            try {
                String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8")
                        .replace("+", "%20");
                generatePresignedUrlRequest.addQueryParameter("response-content-disposition",
                        "attachment; filename*=UTF-8''" + encodedFileName);
            } catch (Exception e) {
                log.warn("文件名编码失败，使用默认文件名: {}", e.getMessage());
            }
        }

        URL presignedUrl = client.generatePresignedUrl(generatePresignedUrlRequest);

        return OSSResponse.success()
                .setFileUrl(presignedUrl.toString())
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("expiration", expirationDate.getTime()));
    }

    @Override
    protected OSSResponse doCopyFile(OSS client, OSSRequest request, String sourceObjectKey, String targetObjectKey) throws Exception {
        CopyObjectRequest copyRequest = new CopyObjectRequest(request.getBucketName(), sourceObjectKey,
                request.getBucketName(), targetObjectKey);
        client.copyObject(copyRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(targetObjectKey)
                .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
    }

    @Override
    protected OSSResponse doCreateBucket(OSS client, OSSRequest request) throws Exception {
        CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
        client.createBucket(createRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doDeleteBucket(OSS client, OSSRequest request) throws Exception {
        client.deleteBucket(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doBucketExists(OSS client, OSSRequest request) throws Exception {
        boolean exists = client.doesBucketExist(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doEnsureBucketExists(OSS client, OSSRequest request) throws Exception {
        boolean bucketExists = client.doesBucketExist(request.getBucketName());
        if (!bucketExists) {
            log.info("桶 {} 不存在，尝试创建", request.getBucketName());

            try {
                CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
                client.createBucket(createRequest);
                log.info("成功创建桶: {}", request.getBucketName());
            } catch (Exception createException) {
                String errorMsg = createException.getMessage();
                log.error("创建桶失败: {}", errorMsg, createException);

                if (errorMsg.contains("BucketAlreadyExists") || errorMsg.contains("already exists")) {
                    return OSSResponse.failure("BUCKET_ALREADY_EXISTS",
                            String.format("桶名 '%s' 已被其他用户使用，请更换桶名", request.getBucketName()));
                } else {
                    return OSSResponse.failure("CREATE_BUCKET_FAILED",
                            String.format("创建桶失败: %s", errorMsg));
                }
            }
        }

        return OSSResponse.success();
    }
}
