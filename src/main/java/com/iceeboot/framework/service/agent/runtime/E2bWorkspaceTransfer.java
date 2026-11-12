package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import io.agentscope.extensions.sandbox.e2b.E2bSandboxClientOptions;
import io.agentscope.extensions.sandbox.e2b.E2bSandboxState;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

/** 使用 envd 文件接口批量上传压缩工作区，避免逐 4000 字符启动一次远程进程。 */
@Slf4j
final class E2bWorkspaceTransfer {
    private static final int ENVD_PORT = 49983;
    private static final OkHttpClient DEFAULT_HTTP = new OkHttpClient();
    private E2bWorkspaceTransfer() {}

    static String upload(byte[] tar, E2bSandboxState state, E2bSandboxClientOptions options) {
        long started = System.nanoTime();
        String remotePath = "/tmp/iceeboot-workspace-" + UUID.randomUUID() + ".tar.gz";
        try {
            var bytes = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(bytes)) { gzip.write(tar); }
            byte[] compressed = bytes.toByteArray();
            String user = options.getRunUser() == null ? "user" : options.getRunUser();
            String domain = state.getSandboxDomain() == null || state.getSandboxDomain().isBlank()
                    ? "e2b.app" : state.getSandboxDomain();
            var url = new HttpUrl.Builder().scheme("https")
                    .host(ENVD_PORT + "-" + state.getSandboxId() + "." + domain)
                    .addPathSegment("files").addQueryParameter("path", remotePath)
                    .addQueryParameter("username", user).build();
            var body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", "workspace.tar.gz", RequestBody.create(compressed, MediaType.get("application/gzip"))).build();
            var request = new Request.Builder().url(url).post(body)
                    .header("Authorization", Credentials.basic(user, ""))
                    .header("E2b-Sandbox-Id", state.getSandboxId())
                    .header("E2b-Sandbox-Port", String.valueOf(ENVD_PORT));
            if (state.getEnvdAccessToken() != null && !state.getEnvdAccessToken().isBlank())
                request.header("X-Access-Token", state.getEnvdAccessToken());
            var http = (options.getHttpClient() == null ? DEFAULT_HTTP : options.getHttpClient()).newBuilder()
                    .connectTimeout(options.getConnectTimeoutSeconds(), TimeUnit.SECONDS)
                    .readTimeout(options.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                    .writeTimeout(options.getReadTimeoutSeconds(), TimeUnit.SECONDS)
                    .callTimeout(120, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build();
            try (var response = http.newCall(request.build()).execute()) {
                if (!response.isSuccessful())
                    throw new IceeBootException(ResponseMessage.Agent.SANDBOX_WORKSPACE_UPLOAD_FAILED);
            }
            log.info("agent_workspace_upload session={} archiveBytes={} uploadedBytes={} elapsedMs={}",
                    state.getSessionId(), tar.length, compressed.length, (System.nanoTime() - started) / 1_000_000);
            return remotePath;
        } catch (IOException failure) {
            throw new IceeBootException(ResponseMessage.Agent.SANDBOX_WORKSPACE_UPLOAD_FAILED, failure);
        }
    }
}
