package com.iceeboot.common.service.oss.service.impl;

import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.AbstractOSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;

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
 * 腾讯云COS服务实现类
 * 使用腾讯云官方SDK实现
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class TencentCOSService extends AbstractOSSService<COSClient> {

    private static final String PROVIDER_NAME = "tencent";
    private static final String DEFAULT_REGION = "ap-beijing";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "腾讯云COS";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_REGION;
    }

    @Override
    protected COSClient createClient(OSSRequest request) {
        COSCredentials cred = new BasicCOSCredentials(request.getAccessKeyId(), request.getAccessKeySecret());
        String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
        Region region = new Region(regionName);
        ClientConfig clientConfig = new ClientConfig(region);
        return new COSClient(cred, clientConfig);
    }

    @Override
    protected void shutdownClient(COSClient client) {
        client.shutdown();
    }

    @Override
    protected String buildFileUrl(OSSRequest request) {
        String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";

        if (request.getCustomDomain() != null) {
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s://%s-%s.cos.%s.myqcloud.com/%s",
                    protocol, request.getBucketName(), request.getAccessKeyId().substring(0, 8), regionName, request.getObjectKey());
        }
    }

    @Override
    protected OSSResponse doUploadFile(COSClient client, OSSRequest request) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(request.getContent().length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
        PutObjectRequest putRequest = new PutObjectRequest(request.getBucketName(), request.getObjectKey(), inputStream, metadata);
        client.putObject(putRequest);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doUploadFile(COSClient client, OSSRequest request, byte[] content) throws Exception {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        PutObjectRequest putRequest = new PutObjectRequest(request.getBucketName(), request.getObjectKey(), inputStream, metadata);
        client.putObject(putRequest);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doUploadFileFromStream(COSClient client, OSSRequest request, InputStream inputStream) throws Exception {
        byte[] content = inputStream.readAllBytes();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(content.length);
        if (request.getContentType() != null) {
            metadata.setContentType(request.getContentType());
        }

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
        PutObjectRequest putRequest = new PutObjectRequest(request.getBucketName(), request.getObjectKey(), byteInputStream, metadata);
        client.putObject(putRequest);

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDownloadFile(COSClient client, OSSRequest request) throws Exception {
        GetObjectRequest getRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
        COSObject cosObject = client.getObject(getRequest);
        byte[] content = cosObject.getObjectContent().readAllBytes();

        return OSSResponse.success()
                .setContent(content)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDownloadFileToLocal(COSClient client, OSSRequest request) throws Exception {
        GetObjectRequest getRequest = new GetObjectRequest(request.getBucketName(), request.getObjectKey());
        File localFile = new File(request.getLocalFilePath());
        client.getObject(getRequest, localFile);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("localPath", request.getLocalFilePath()));
    }

    @Override
    protected OSSResponse doDeleteFile(COSClient client, OSSRequest request) throws Exception {
        client.deleteObject(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDeleteFiles(COSClient client, OSSRequest request, List<String> objectKeys) throws Exception {
        DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(request.getBucketName());
        List<DeleteObjectsRequest.KeyVersion> keys = objectKeys.stream()
                .map(key -> new DeleteObjectsRequest.KeyVersion(key))
                .collect(Collectors.toList());
        deleteRequest.setKeys(keys);

        DeleteObjectsResult result = client.deleteObjects(deleteRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("deletedCount", result.getDeletedObjects().size()));
    }

    @Override
    protected OSSResponse doFileExists(COSClient client, OSSRequest request) throws Exception {
        boolean exists = client.doesObjectExist(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doGetFileInfo(COSClient client, OSSRequest request) throws Exception {
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
    protected OSSResponse doListFiles(COSClient client, OSSRequest request) throws Exception {
        ListObjectsRequest listRequest = new ListObjectsRequest();
        listRequest.setBucketName(request.getBucketName());
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
    protected OSSResponse doGeneratePresignedUrl(COSClient client, OSSRequest request, Long expiration) throws Exception {
        GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(
                request.getBucketName(), request.getObjectKey(), HttpMethodName.GET);
        Date expirationDate = new Date(System.currentTimeMillis() + expiration * 1000);
        presignedUrlRequest.setExpiration(expirationDate);

        if (request.getOriginalFileName() != null && !request.getOriginalFileName().isEmpty()) {
            try {
                String encodedFileName = java.net.URLEncoder.encode(request.getOriginalFileName(), "UTF-8");
                presignedUrlRequest.addRequestParameter("response-content-disposition", "attachment; filename*=UTF-8''" + encodedFileName);
            } catch (Exception e) {
                log.warn("编码文件名失败: {}", e.getMessage());
            }
        }

        URL presignedUrl = client.generatePresignedUrl(presignedUrlRequest);

        return OSSResponse.success()
                .setFileUrl(presignedUrl.toString())
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("expiration", expirationDate.getTime()));
    }

    @Override
    protected OSSResponse doCopyFile(COSClient client, OSSRequest request, String sourceObjectKey, String targetObjectKey) throws Exception {
        CopyObjectRequest copyRequest = new CopyObjectRequest(
                request.getBucketName(), sourceObjectKey, request.getBucketName(), targetObjectKey);
        client.copyObject(copyRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(targetObjectKey)
                .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
    }

    @Override
    protected OSSResponse doCreateBucket(COSClient client, OSSRequest request) throws Exception {
        CreateBucketRequest createRequest = new CreateBucketRequest(request.getBucketName());
        client.createBucket(createRequest);

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doDeleteBucket(COSClient client, OSSRequest request) throws Exception {
        client.deleteBucket(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doBucketExists(COSClient client, OSSRequest request) throws Exception {
        boolean exists = client.doesBucketExist(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doEnsureBucketExists(COSClient client, OSSRequest request) throws Exception {
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
