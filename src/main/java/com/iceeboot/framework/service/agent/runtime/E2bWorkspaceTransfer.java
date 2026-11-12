package com.iceeboot.framework.service.agent.runtime;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.service.agent.IAgentFileService;
import io.agentscope.extensions.sandbox.e2b.E2bSandboxClientOptions;
import io.agentscope.extensions.sandbox.e2b.E2bSandboxState;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

/** 工作区归档批量上传，文件交付使用二进制接口，避免 Shell 输出截断。 */
@Slf4j
final class E2bWorkspaceTransfer {
    private static final int ENVD_PORT = 49983;
    private static final OkHttpClient DEFAULT_HTTP = new OkHttpClient();
    private E2bWorkspaceTransfer() {}
    private static Request.Builder request(String path,E2bSandboxState state,E2bSandboxClientOptions options) {
        String user=options.getRunUser()==null ? "user" : options.getRunUser();
        String domain=state.getSandboxDomain()==null || state.getSandboxDomain().isBlank() ? "e2b.app" : state.getSandboxDomain();
        var url=new HttpUrl.Builder().scheme("https").host(ENVD_PORT+"-"+state.getSandboxId()+"."+domain)
                .addPathSegment("files").addQueryParameter("path",path).addQueryParameter("username",user).build();
        var request=new Request.Builder().url(url).header("Authorization",Credentials.basic(user,""))
                .header("E2b-Sandbox-Id",state.getSandboxId()).header("E2b-Sandbox-Port",String.valueOf(ENVD_PORT));
        if(state.getEnvdAccessToken()!=null && !state.getEnvdAccessToken().isBlank()) request.header("X-Access-Token",state.getEnvdAccessToken());
        return request;
    }
    private static OkHttpClient http(E2bSandboxClientOptions options) {
        return (options.getHttpClient()==null ? DEFAULT_HTTP : options.getHttpClient()).newBuilder()
                .connectTimeout(options.getConnectTimeoutSeconds(),TimeUnit.SECONDS)
                .readTimeout(options.getReadTimeoutSeconds(),TimeUnit.SECONDS)
                .writeTimeout(options.getReadTimeoutSeconds(),TimeUnit.SECONDS)
                .callTimeout(120,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build();
    }
    static void uploadFile(String path,byte[] content,E2bSandboxState state,E2bSandboxClientOptions options) {
        var body=new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file","upload",RequestBody.create(content,MediaType.get("application/octet-stream"))).build();
        try(var response=http(options).newCall(request(path,state,options).post(body).build()).execute()) {
            if(!response.isSuccessful()) throw new IceeBootException(ResponseMessage.Agent.SANDBOX_WORKSPACE_UPLOAD_FAILED);
        } catch(IOException failure) { throw new IceeBootException(ResponseMessage.Agent.SANDBOX_WORKSPACE_UPLOAD_FAILED,failure); }
    }
    static byte[] downloadFile(String path,E2bSandboxState state,E2bSandboxClientOptions options) {
        try(var response=http(options).newCall(request(path,state,options).get().build()).execute()) {
            if(!response.isSuccessful() || response.body()==null) throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED);
            if(response.body().contentLength()>IAgentFileService.MAX_BYTES) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
            byte[] bytes=response.body().byteStream().readNBytes((int)IAgentFileService.MAX_BYTES+1);
            if(bytes.length>IAgentFileService.MAX_BYTES) throw new IceeBootException(ResponseMessage.Agent.FILE_INVALID);
            return bytes;
        } catch(IOException failure) { throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED,failure); }
    }
    static String upload(byte[] tar,E2bSandboxState state,E2bSandboxClientOptions options) {
        long started=System.nanoTime();
        String path="/tmp/iceeboot-workspace-"+UUID.randomUUID()+".tar.gz";
        try {
            var bytes=new ByteArrayOutputStream();
            try(var gzip=new GZIPOutputStream(bytes)) { gzip.write(tar); }
            byte[] compressed=bytes.toByteArray();
            uploadFile(path,compressed,state,options);
            log.info("agent_workspace_upload session={} archiveBytes={} uploadedBytes={} elapsedMs={}",
                    state.getSessionId(),tar.length,compressed.length,(System.nanoTime()-started)/1_000_000);
            return path;
        } catch(IOException failure) { throw new IceeBootException(ResponseMessage.Agent.SANDBOX_WORKSPACE_UPLOAD_FAILED,failure); }
    }
}
