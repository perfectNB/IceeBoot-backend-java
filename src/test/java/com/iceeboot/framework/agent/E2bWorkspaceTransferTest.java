package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.framework.service.agent.runtime.ReleasedE2bFilesystemSpec;
import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.sandbox.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import okhttp3.*;
import org.junit.jupiter.api.Test;

/** 真实经过 SDK 工作区生命周期，HTTP 在内存中拦截，不访问云服务。 */
class E2bWorkspaceTransferTest {
    @Test void sendsWholeArchiveInOneUploadAndOneExtractCommand() throws Exception {
        var fixture = new Fixture(200);
        var sandbox = fixture.sandbox();
        sandbox.start();
        fixture.commands.clear();
        byte[] tar = archive();
        sandbox.hydrateWorkspace(new ByteArrayInputStream(tar));
        assertThat(fixture.uploads.get()).isEqualTo(1);
        assertThat(fixture.commands).hasSize(1);
        assertThat(fixture.commands.getFirst()).contains("tar", "/home/user/workspace", "rm").doesNotContain("base64");
        try (var gzip = new GZIPInputStream(new ByteArrayInputStream(fixture.uploaded.get()))) {
            assertThat(gzip.readAllBytes()).isEqualTo(tar);
        }
        assertThat(fixture.uploaded.get().length).isLessThan(tar.length / 2);
    }

    @Test void failedUploadDoesNotExtractOrExposeServerSecrets() throws Exception {
        var fixture = new Fixture(401);
        var sandbox = fixture.sandbox();
        sandbox.start();
        fixture.commands.clear();
        assertThatThrownBy(() -> sandbox.hydrateWorkspace(new ByteArrayInputStream(archive())))
                .satisfies(error -> {
                    String messages = error.toString();
                    for(Throwable cause=error.getCause();cause!=null;cause=cause.getCause()) messages += cause;
                    assertThat(messages).doesNotContain("server-private-token");
                });
        assertThat(fixture.uploads.get()).isEqualTo(1);
        assertThat(fixture.commands).isEmpty();
    }

    @Test void nativeSnapshotReferencesContinueThroughSdkWithoutUploadingFiles() throws Exception {
        var fixture = new Fixture(200);
        var sandbox = fixture.sandbox();
        sandbox.start();
        sandbox.hydrateWorkspace(new ByteArrayInputStream(E2bSnapshotRefs.encodeSnapshotId("fixture-snapshot")));
        assertThat(fixture.uploads.get()).isZero();
        assertThat(fixture.commands).noneMatch(command -> command.contains("base64") || command.contains("tar -xzf"));
    }

    @Test void sdkNativeBinaryDownloadsAvoidShellTruncationAndEnforceTwentyMibLimit() throws Exception {
        var fixture=new Fixture(200);
        fixture.downloaded=new byte[1024*1024];new Random(11).nextBytes(fixture.downloaded);
        var sandbox=fixture.sandbox();sandbox.start();fixture.commands.clear();
        var filesystem=new io.agentscope.harness.agent.filesystem.sandbox.PinnedSandboxFilesystem(sandbox);
        var result=filesystem.downloadFiles(null,List.of("/home/user/workspace/report.bin")).getFirst();
        assertThat(result.isSuccess()).isTrue();assertThat(result.content()).isEqualTo(fixture.downloaded);
        assertThat(fixture.commands).isEmpty();
        fixture.downloaded=new byte[20*1024*1024+1];
        assertThat(filesystem.downloadFiles(null,List.of("/home/user/workspace/too-big")).getFirst().isSuccess()).isFalse();
        assertThat(filesystem.downloadFiles(null,List.of("/etc/passwd")).getFirst().isSuccess()).isFalse();
    }

    private byte[] archive() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try(var tar = new org.apache.commons.compress.archivers.tar.TarArchiveOutputStream(bytes)) {
            tar.setLongFileMode(org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX);
            byte[] content="中文技能文件，print('hello')\n".repeat(20000).getBytes(StandardCharsets.UTF_8);
            var entry=new org.apache.commons.compress.archivers.tar.TarArchiveEntry(".skills-cache/skill/说明.md");
            entry.setSize(content.length);tar.putArchiveEntry(entry);tar.write(content);tar.closeArchiveEntry();
        }
        return bytes.toByteArray();
    }

    private static class Fixture {
        final AtomicInteger uploads = new AtomicInteger();
        final AtomicReference<byte[]> uploaded = new AtomicReference<>();
        final List<String> commands = new ArrayList<>();
        final ObjectMapper json = new ObjectMapper();
        byte[] downloaded;
        final int uploadStatus;
        Fixture(int status) { uploadStatus=status; }
        Sandbox sandbox() {
            var http = new OkHttpClient.Builder().addInterceptor(chain -> {
                var request = chain.request();
                if(request.url().encodedPath().equals("/files") && request.method().equals("GET")) {
                    assertThat(request.header("X-Access-Token")).isEqualTo("fixture-envd-token");
                    assertThat(request.url().queryParameter("path")).startsWith("/home/user/workspace/");
                    return response(request,200,downloaded);
                }
                if(request.url().encodedPath().equals("/files")) {
                    uploads.incrementAndGet();
                    assertThat(request.header("X-Access-Token")).isEqualTo("fixture-envd-token");
                    assertThat(request.header("E2b-Sandbox-Id")).isEqualTo("fixture-sandbox");
                    assertThat(request.url().queryParameter("path")).startsWith("/tmp/").endsWith(".tar.gz");
                    var body=(MultipartBody)request.body();
                    var buffer = new okio.Buffer();body.part(0).body().writeTo(buffer);uploaded.set(buffer.readByteArray());
                    return response(request,uploadStatus,"server-private-token".getBytes(StandardCharsets.UTF_8));
                }
                if(request.url().encodedPath().endsWith("/process.Process/Start")) {
                    var buffer=new okio.Buffer();request.body().writeTo(buffer);byte[] bytes=buffer.readByteArray();
                    var process=json.readTree(Arrays.copyOfRange(bytes,5,bytes.length)).path("process");
                    var args=process.path("args");
                    commands.add(args.get(args.size()-1).asText());
                    byte[] end="{\"event\":{\"end\":{\"exitCode\":0}}}".getBytes(StandardCharsets.UTF_8);
                    return response(request,200,ByteBuffer.allocate(5+end.length).put((byte)0).putInt(end.length).put(end).array());
                }
                return response(request,200,"{\"sandboxID\":\"fixture-sandbox\",\"envdAccessToken\":\"fixture-envd-token\"}".getBytes(StandardCharsets.UTF_8));
            }).build();
            var workspace=new WorkspaceSpec();workspace.setRoot("/home/user/workspace");
            var ctx=new ReleasedE2bFilesystemSpec().apiKey("fixture-key").apiBaseUrl("https://platform.example.invalid")
                    .domain("example.invalid").workspaceSpec(workspace).workspaceRoot(workspace.getRoot()).codec(E2bCodec.JSON).toSandboxContext();
            ((E2bSandboxClientOptions)ctx.getClientOptions()).setHttpClient(http);
            var state=new E2bSandboxState();state.setSessionId("fixture-session");state.setSandboxId("fixture-sandbox");
            state.setSandboxDomain("example.invalid");state.setEnvdAccessToken("fixture-envd-token");state.setWorkspaceSpec(workspace);
            state.setWorkspaceRoot(workspace.getRoot());state.setCodec(E2bCodec.JSON);state.setPersistenceMode(E2bPersistenceMode.TAR);
            return ctx.getClient().resume(state);
        }
        private Response response(Request request,int status,byte[] body) {
            return new Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1).code(status).message("fixture")
                    .body(ResponseBody.create(body,MediaType.get("application/connect+json"))).build();
        }
    }
}
