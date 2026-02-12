package com.iceeboot.common.service.oss.service.impl;

import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.AbstractOSSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MinIO服务实现类
 * 使用MinIO官方SDK实现
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
@Component
public class MinIOService extends AbstractOSSService<MinioClient> {

    private static final String PROVIDER_NAME = SystemConstants.OSSProvider.MINIO;
    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "MinIO";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected MinioClient createClient(OSSRequest request) {
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
    protected void shutdownClient(MinioClient client) {
        // MinioClient does not have a close/shutdown method
    }

    @Override
    protected String buildFileUrl(OSSRequest request) {
        String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;

        if (request.getCustomDomain() != null) {
            String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s/%s/%s", endpoint, request.getBucketName(), request.getObjectKey());
        }
    }

    @Override
    protected OSSResponse doUploadFile(MinioClient client, OSSRequest request) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());

        PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .stream(inputStream, request.getContent().length, -1);

        if (request.getContentType() != null) {
            putObjectBuilder.contentType(request.getContentType());
        }

        ObjectWriteResponse response = client.putObject(putObjectBuilder.build());

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("etag", response.etag(), "versionId", response.versionId() != null ? response.versionId() : ""));
    }

    @Override
    protected OSSResponse doUploadFile(MinioClient client, OSSRequest request, byte[] content) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);

        PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .stream(inputStream, content.length, -1);

        if (request.getContentType() != null) {
            putObjectBuilder.contentType(request.getContentType());
        }

        ObjectWriteResponse response = client.putObject(putObjectBuilder.build());

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("etag", response.etag(), "versionId", response.versionId() != null ? response.versionId() : ""));
    }

    @Override
    protected OSSResponse doUploadFileFromStream(MinioClient client, OSSRequest request, InputStream inputStream) throws Exception {
        byte[] content = inputStream.readAllBytes();
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);

        PutObjectArgs.Builder putObjectBuilder = PutObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .stream(byteInputStream, content.length, -1);

        if (request.getContentType() != null) {
            putObjectBuilder.contentType(request.getContentType());
        }

        ObjectWriteResponse response = client.putObject(putObjectBuilder.build());

        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("etag", response.etag(), "versionId", response.versionId() != null ? response.versionId() : ""));
    }

    @Override
    protected OSSResponse doDownloadFile(MinioClient client, OSSRequest request) throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .build();

        try (InputStream is = client.getObject(getObjectArgs);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] content = baos.toByteArray();

            return OSSResponse.success()
                    .setContent(content)
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey());
        }
    }

    @Override
    protected OSSResponse doDownloadFileToLocal(MinioClient client, OSSRequest request) throws Exception {
        GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .build();

        try (InputStream is = client.getObject(getObjectArgs);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            java.nio.file.Files.write(
                    java.nio.file.Paths.get(request.getLocalFilePath()),
                    baos.toByteArray()
            );

            return OSSResponse.success()
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey())
                    .setExtraData(Map.of("localPath", request.getLocalFilePath()));
        }
    }

    @Override
    protected OSSResponse doDeleteFile(MinioClient client, OSSRequest request) throws Exception {
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .build();

        client.removeObject(removeObjectArgs);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDeleteFiles(MinioClient client, OSSRequest request, List<String> objectKeys) throws Exception {
        List<DeleteObject> objects = new ArrayList<>();
        for (String key : objectKeys) {
            objects.add(new DeleteObject(key));
        }

        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder()
                .bucket(request.getBucketName())
                .objects(objects)
                .build();

        Iterable<Result<DeleteError>> results = client.removeObjects(removeObjectsArgs);

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
                deletedCount++;
            }
        }

        Map<String, Object> extraData = new HashMap<>();
        extraData.put("deletedCount", deletedCount);
        extraData.put("totalCount", objectKeys.size());
        if (!errors.isEmpty()) {
            extraData.put("errors", errors);
        }

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(extraData);
    }

    @Override
    protected OSSResponse doFileExists(MinioClient client, OSSRequest request) throws Exception {
        try {
            StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                    .bucket(request.getBucketName())
                    .object(request.getObjectKey())
                    .build();

            client.statObject(statObjectArgs);

            return OSSResponse.success()
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey())
                    .setExtraData(Map.of("exists", true));
        } catch (Exception e) {
            return OSSResponse.success()
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey())
                    .setExtraData(Map.of("exists", false));
        }
    }

    @Override
    protected OSSResponse doGetFileInfo(MinioClient client, OSSRequest request) throws Exception {
        StatObjectArgs statObjectArgs = StatObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .build();

        StatObjectResponse stat = client.statObject(statObjectArgs);

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
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(info);
    }

    @Override
    protected OSSResponse doListFiles(MinioClient client, OSSRequest request) throws Exception {
        ListObjectsArgs.Builder listObjectsBuilder = ListObjectsArgs.builder()
                .bucket(request.getBucketName());

        if (request.getObjectKey() != null) {
            listObjectsBuilder.prefix(request.getObjectKey());
        }

        Iterable<Result<Item>> results = client.listObjects(listObjectsBuilder.build());

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
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("files", files));
    }

    @Override
    protected OSSResponse doGeneratePresignedUrl(MinioClient client, OSSRequest request, Long expiration) throws Exception {
        GetPresignedObjectUrlArgs.Builder argsBuilder = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(request.getBucketName())
                .object(request.getObjectKey())
                .expiry(expiration.intValue(), TimeUnit.SECONDS);

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

        String presignedUrl = client.getPresignedObjectUrl(argsBuilder.build());

        return OSSResponse.success()
                .setFileUrl(presignedUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("expiration", System.currentTimeMillis() + expiration * 1000));
    }

    @Override
    protected OSSResponse doCopyFile(MinioClient client, OSSRequest request, String sourceObjectKey, String targetObjectKey) throws Exception {
        CopySource copySource = CopySource.builder()
                .bucket(request.getBucketName())
                .object(sourceObjectKey)
                .build();

        CopyObjectArgs copyObjectArgs = CopyObjectArgs.builder()
                .bucket(request.getBucketName())
                .object(targetObjectKey)
                .source(copySource)
                .build();

        ObjectWriteResponse response = client.copyObject(copyObjectArgs);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(targetObjectKey)
                .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey, "etag", response.etag()));
    }

    @Override
    protected OSSResponse doCreateBucket(MinioClient client, OSSRequest request) throws Exception {
        MakeBucketArgs.Builder makeBucketBuilder = MakeBucketArgs.builder()
                .bucket(request.getBucketName());

        if (request.getRegion() != null) {
            makeBucketBuilder.region(request.getRegion());
        }

        client.makeBucket(makeBucketBuilder.build());

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doDeleteBucket(MinioClient client, OSSRequest request) throws Exception {
        RemoveBucketArgs removeBucketArgs = RemoveBucketArgs.builder()
                .bucket(request.getBucketName())
                .build();

        client.removeBucket(removeBucketArgs);

        return OSSResponse.success()
                .setBucketName(request.getBucketName());
    }

    @Override
    protected OSSResponse doBucketExists(MinioClient client, OSSRequest request) throws Exception {
        BucketExistsArgs bucketExistsArgs = BucketExistsArgs.builder()
                .bucket(request.getBucketName())
                .build();

        boolean exists = client.bucketExists(bucketExistsArgs);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doEnsureBucketExists(MinioClient client, OSSRequest request) throws Exception {
        boolean bucketExists = client.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(request.getBucketName())
                        .build()
        );

        if (!bucketExists) {
            log.info("桶 {} 不存在，尝试创建", request.getBucketName());

            try {
                MakeBucketArgs.Builder makeBucketBuilder = MakeBucketArgs.builder()
                        .bucket(request.getBucketName());

                if (request.getRegion() != null) {
                    makeBucketBuilder.region(request.getRegion());
                }

                client.makeBucket(makeBucketBuilder.build());
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
