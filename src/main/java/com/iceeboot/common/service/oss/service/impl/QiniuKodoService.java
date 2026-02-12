package com.iceeboot.common.service.oss.service.impl;

import com.google.gson.Gson;
import com.iceeboot.common.service.oss.dto.OSSRequest;
import com.iceeboot.common.service.oss.dto.OSSResponse;
import com.iceeboot.common.service.oss.service.AbstractOSSService;
import lombok.extern.slf4j.Slf4j;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.DefaultPutRet;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.util.Auth;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

/**
 * 七牛云Kodo服务实现类
 * 使用七牛云官方SDK实现
 *
 * @author CodeIcee
 * @date 2025-09-03
 */
@Slf4j
public class QiniuKodoService extends AbstractOSSService<Auth> {

    private static final String PROVIDER_NAME = "qiniu";
    private static final String DEFAULT_REGION = "z0";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    protected String getProviderDisplayName() {
        return "七牛云Kodo";
    }

    @Override
    protected String getDefaultEndpoint() {
        return DEFAULT_REGION;
    }

    @Override
    protected Auth createClient(OSSRequest request) {
        return Auth.create(request.getAccessKeyId(), request.getAccessKeySecret());
    }

    @Override
    protected void shutdownClient(Auth client) {
        // Auth does not require shutdown
    }

    @Override
    protected String buildFileUrl(OSSRequest request) {
        String protocol = (request.getUseHttps() != null && !request.getUseHttps()) ? "http" : "https";

        if (request.getCustomDomain() != null) {
            return String.format("%s://%s/%s", protocol, request.getCustomDomain(), request.getObjectKey());
        } else {
            return String.format("%s://%s.qiniudn.com/%s", protocol, request.getBucketName(), request.getObjectKey());
        }
    }

    private Configuration createConfiguration(OSSRequest request) {
        Configuration cfg = new Configuration();

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
    protected OSSResponse doUploadFile(Auth client, OSSRequest request) throws Exception {
        String upToken = client.uploadToken(request.getBucketName());
        Configuration cfg = createConfiguration(request);
        UploadManager uploadManager = new UploadManager(cfg);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(request.getContent());
        Response response = uploadManager.put(inputStream, request.getObjectKey(), upToken, null, request.getContentType());

        DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
        String fileUrl = buildFileUrl(request);

        return OSSResponse.success()
                .setFileUrl(fileUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("hash", putRet.hash, "key", putRet.key));
    }

    @Override
    protected OSSResponse doUploadFile(Auth client, OSSRequest request, byte[] content) throws Exception {
        String upToken = client.uploadToken(request.getBucketName());
        Configuration cfg = createConfiguration(request);
        UploadManager uploadManager = new UploadManager(cfg);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
        Response response = uploadManager.put(inputStream, request.getObjectKey(), upToken, null, request.getContentType());

        if (response.isOK()) {
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            String fileUrl = buildFileUrl(request);

            return OSSResponse.success()
                    .setFileUrl(fileUrl)
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey());
        } else {
            return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + response.error);
        }
    }

    @Override
    protected OSSResponse doUploadFileFromStream(Auth client, OSSRequest request, InputStream inputStream) throws Exception {
        byte[] content = inputStream.readAllBytes();

        String upToken = client.uploadToken(request.getBucketName());
        Configuration cfg = createConfiguration(request);
        UploadManager uploadManager = new UploadManager(cfg);

        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(content);
        Response response = uploadManager.put(byteInputStream, request.getObjectKey(), upToken, null, request.getContentType());

        if (response.isOK()) {
            DefaultPutRet putRet = new Gson().fromJson(response.bodyString(), DefaultPutRet.class);
            String fileUrl = buildFileUrl(request);

            return OSSResponse.success()
                    .setFileUrl(fileUrl)
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey());
        } else {
            return OSSResponse.failure("UPLOAD_FAILED", "上传失败: " + response.error);
        }
    }

    @Override
    protected OSSResponse doDownloadFile(Auth client, OSSRequest request) throws Exception {
        String privateUrl = client.privateDownloadUrl(buildFileUrl(request));

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
                    .setContent(content)
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey());
        }
    }

    @Override
    protected OSSResponse doDownloadFileToLocal(Auth client, OSSRequest request) throws Exception {
        String privateUrl = client.privateDownloadUrl(buildFileUrl(request));

        java.net.URL url = new java.net.URL(privateUrl);
        try (InputStream is = url.openStream();
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
    protected OSSResponse doDeleteFile(Auth client, OSSRequest request) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        bucketManager.delete(request.getBucketName(), request.getObjectKey());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey());
    }

    @Override
    protected OSSResponse doDeleteFiles(Auth client, OSSRequest request, List<String> objectKeys) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        BucketManager.BatchOperations batchOperations = new BucketManager.BatchOperations();
        for (String key : objectKeys) {
            batchOperations.addDeleteOp(request.getBucketName(), key);
        }

        Response response = bucketManager.batch(batchOperations);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("deletedCount", objectKeys.size(), "batchResponse", response.bodyString()));
    }

    @Override
    protected OSSResponse doFileExists(Auth client, OSSRequest request) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        try {
            bucketManager.stat(request.getBucketName(), request.getObjectKey());

            return OSSResponse.success()
                    .setBucketName(request.getBucketName())
                    .setObjectKey(request.getObjectKey())
                    .setExtraData(Map.of("exists", true));
        } catch (QiniuException e) {
            if (e.response != null && e.response.statusCode == 612) {
                return OSSResponse.success()
                        .setBucketName(request.getBucketName())
                        .setObjectKey(request.getObjectKey())
                        .setExtraData(Map.of("exists", false));
            } else {
                return OSSResponse.failure("FILE_EXISTS_CHECK_FAILED", "检查文件是否存在失败: " + e.getMessage());
            }
        }
    }

    @Override
    protected OSSResponse doGetFileInfo(Auth client, OSSRequest request) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        FileInfo fileInfo = bucketManager.stat(request.getBucketName(), request.getObjectKey());

        Map<String, Object> info = new HashMap<>();
        info.put("size", fileInfo.fsize);
        info.put("hash", fileInfo.hash);
        info.put("mimeType", fileInfo.mimeType);
        info.put("putTime", fileInfo.putTime);
        info.put("type", fileInfo.type);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(info);
    }

    @Override
    protected OSSResponse doListFiles(Auth client, OSSRequest request) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

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
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("files", files));
    }

    @Override
    protected OSSResponse doGeneratePresignedUrl(Auth client, OSSRequest request, Long expiration) throws Exception {
        String baseUrl = buildFileUrl(request);
        long deadline = System.currentTimeMillis() / 1000 + expiration;
        String privateUrl = client.privateDownloadUrlWithDeadline(baseUrl, deadline);

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
                .setFileUrl(privateUrl)
                .setBucketName(request.getBucketName())
                .setObjectKey(request.getObjectKey())
                .setExtraData(Map.of("expiration", deadline * 1000));
    }

    @Override
    protected OSSResponse doCopyFile(Auth client, OSSRequest request, String sourceObjectKey, String targetObjectKey) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        bucketManager.copy(request.getBucketName(), sourceObjectKey, request.getBucketName(), targetObjectKey);

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setObjectKey(targetObjectKey)
                .setExtraData(Map.of("sourceKey", sourceObjectKey, "targetKey", targetObjectKey));
    }

    @Override
    protected OSSResponse doCreateBucket(Auth client, OSSRequest request) throws Exception {
        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("message", "七牛云存储空间需要通过控制台创建"));
    }

    @Override
    protected OSSResponse doDeleteBucket(Auth client, OSSRequest request) throws Exception {
        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("message", "七牛云存储空间需要通过控制台删除"));
    }

    @Override
    protected OSSResponse doBucketExists(Auth client, OSSRequest request) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        String[] buckets = bucketManager.buckets();
        boolean exists = Arrays.asList(buckets).contains(request.getBucketName());

        return OSSResponse.success()
                .setBucketName(request.getBucketName())
                .setExtraData(Map.of("exists", exists));
    }

    @Override
    protected OSSResponse doEnsureBucketExists(Auth client, OSSRequest request) throws Exception {
        Configuration cfg = createConfiguration(request);
        BucketManager bucketManager = new BucketManager(client, cfg);

        try {
            bucketManager.getBucketInfo(request.getBucketName());
            return OSSResponse.success();
        } catch (QiniuException e) {
            if (e.code() == 631) {
                log.info("桶 {} 不存在，尝试创建", request.getBucketName());

                try {
                    String regionName = request.getRegion() != null ? request.getRegion() : DEFAULT_REGION;
                    bucketManager.createBucket(request.getBucketName(), regionName);
                    log.info("成功创建桶: {}", request.getBucketName());
                    return OSSResponse.success();
                } catch (QiniuException createException) {
                    String errorMsg = createException.getMessage();
                    log.error("创建桶失败: {}", errorMsg, createException);

                    if (createException.code() == 614) {
                        return OSSResponse.failure("BUCKET_ALREADY_EXISTS",
                                String.format("桶名 '%s' 已被其他用户使用，请更换桶名", request.getBucketName()));
                    } else {
                        return OSSResponse.failure("CREATE_BUCKET_FAILED",
                                String.format("创建桶失败: %s", errorMsg));
                    }
                }
            } else {
                return OSSResponse.failure("CHECK_BUCKET_FAILED", "检查桶存在性失败: " + e.getMessage());
            }
        }
    }
}
