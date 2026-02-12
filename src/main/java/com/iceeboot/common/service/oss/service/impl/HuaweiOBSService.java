package com.iceeboot.common.service.oss.service.impl;

import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.AbstractOSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.obs.services.ObsClient;
import com.obs.services.model.*;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 华为云OBS服务实现类
 * 使用华为云官方SDK实现
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class HuaweiOBSService extends AbstractOSSService<ObsClient> {

    private static final String PROVIDER_NAME = "huawei";
    private static final String DEFAULT_ENDPOINT = "obs.cn-north-4.myhuaweicloud.com";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "华为云OBS";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected ObsClient createClient(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
        return new ObsClient(request.getAccessKeyId(), request.getAccessKeySecret(), endpoint);
    }

    @Override
    protected void shutdownClient(ObsClient client) {
        try {
            client.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    protected OSSResponse doUploadFile(ObsClient client, OSSRequest request) throws Exception {
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
        client.putObject(putRequest);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doUploadFile(ObsClient client, OSSRequest request, byte[] content) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength((long) content.length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        PutObjectRequest putRequest = new PutObjectRequest();
        putRequest.setBucketName(request.getBucketName());
        putRequest.setObjectKey(request.getObjectKey());
        putRequest.setInput(inputStream);
        putRequest.setMetadata(metadata);
        client.putObject(putRequest);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doUploadFileFromStream(ObsClient client, OSSRequest request, InputStream inputStream) throws Exception {
        byte[] content = inputStream.readAllBytes();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength((long) content.length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
        PutObjectRequest putRequest = new PutObjectRequest();
        putRequest.setBucketName(request.getBucketName());
        putRequest.setObjectKey(request.getObjectKey());
        putRequest.setInput(byteInputStream);
        putRequest.setMetadata(metadata);
        client.putObject(putRequest);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDownloadFile(ObsClient client, OSSRequest request) throws Exception {
        GetObjectRequest getObjectRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
        ObsObject obsObject = client.getObject(getObjectRequest);
        byte[] content = obsObject.getObjectContent().readAllBytes();

        return OSSResponse.success()
                .setContent(content)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDownloadFileToLocal(ObsClient client, OSSRequest request) throws Exception {
        GetObjectRequest getObjectRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
        ObsObject obsObject = client.getObject(getObjectRequest);

        try (InputStream is = obsObject.getObjectContent();
             FileOutputStream outputStream = new FileOutputStream(request.getLocalFilePath())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("localPath", request.getLocalFilePath()));
    }

    @Override
    protected OSSResponse doDeleteFile(ObsClient client, OSSRequest request) throws Exception {
        client.deleteObject(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDeleteFiles(ObsClient client, OSSRequest request, List<String> objectKeys) throws Exception {
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(request.getBucketName());
        KeyAndVersion[] keys = objectKeys.stream()
                .map(key -> new KeyAndVersion(key))
                .toArray(KeyAndVersion[]::new);
        deleteRequest.setKeyAndVersions(keys);

        DeleteObjectsResult result = client.deleteObjects(deleteRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("deletedCount", result.getDeletedObjectResults().size()));
    }

    @Override
    protected OSSResponse doFileExists(ObsClient client, OSSRequest request) throws Exception {
        boolean exists = client.doesObjectExist(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doGetFileInfo(ObsClient client, OSSRequest request) throws Exception {
        ObjectMetadata metadata = client.getObjectMetadata(request.getBucketName(), request.getObjectKey());

        Map<String, Object> fileInfo = new HashMap<>();
        fileInfo.put("size", metadata.getContentLength());
        fileInfo.put("contentType", metadata.getContentType());
        fileInfo.put("lastModified", metadata.getLastModified());
        fileInfo.put("etag", metadata.getEtag());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(fileInfo);
    }

    @Override
    protected OSSResponse doListFiles(ObsClient client, OSSRequest request) throws Exception {
        ListObjectsRequest listRequest = new ListObjectsRequest(request.getBucketName());
        if (request.getObjectKey() != null) {
            listRequest.setPrefix(request.getObjectKey());
        }

        ObjectListing objectListing = client.listObjects(listRequest);

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
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("files", files));
    }

    @Override
    protected OSSResponse doGeneratePresignedUrl(ObsClient client, OSSRequest request, Long expiration) throws Exception {
        TemporarySignatureRequest signatureRequest = new TemporarySignatureRequest(HttpMethodEnum.GET, expiration.intValue());
        signatureRequest.setBucketName(request.getBucketName());
        signatureRequest.setObjectKey(request.getObjectKey());

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

        TemporarySignatureResponse signatureResponse = client.createTemporarySignature(signatureRequest);

        return OSSResponse.success()
                .setFileUrl(signatureResponse.getSignedUrl())
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("expiration", System.currentTimeMillis() + expiration * 1000));
    }

    @Override
    protected OSSResponse doCopyFile(ObsClient client, OSSRequest request, String sourceObjectKey, String targetObjectKey) throws Exception {
        CopyObjectRequest copyRequest = new CopyObjectRequest(request.getBucketName(), sourceObjectKey,
                request.getBucketName(), targetObjectKey);
        client.copyObject(copyRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(targetObjectKey)
                .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
    }

    @Override
    protected OSSResponse doCreateBucket(ObsClient client, OSSRequest request) throws Exception {
        CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
        client.createBucket(createRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doDeleteBucket(ObsClient client, OSSRequest request) throws Exception {
        client.deleteBucket(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doBucketExists(ObsClient client, OSSRequest request) throws Exception {
        boolean exists = client.headBucket(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doEnsureBucketExists(ObsClient client, OSSRequest request) throws Exception {
        boolean bucketExists = client.headBucket(request.getBucketName());
        if (!bucketExists) {
            log.info("桶 {} 不存在，尝试创建", request.getBucketName());

            try {
                CreateBucketRequest createRequest = new CreateBucketRequest();
                createRequest.setBucketName(request.getBucketName());
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
