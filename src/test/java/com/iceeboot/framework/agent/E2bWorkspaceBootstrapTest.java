package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.framework.service.agent.runtime.ReleasedE2bFilesystemSpec;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.extensions.sandbox.e2b.*;
import io.agentscope.harness.agent.sandbox.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** 在内存中模拟 E2B 平台与 envd，不访问真实云端。 */
class E2bWorkspaceBootstrapTest {
    private static final String ROOT = "/home/user/workspace";
    private final ObjectMapper json = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void createsMissingWorkspaceBeforeUsingItAsCwd(boolean resume, boolean directoryExists) throws Exception {
        var exists = new AtomicBoolean(directoryExists);
        var processes = new ArrayList<com.fasterxml.jackson.databind.JsonNode>();
        var http = new OkHttpClient.Builder().addInterceptor(chain -> {
            var request = chain.request();
            if (request.url().encodedPath().endsWith("/process.Process/Start")) {
                var buffer = new okio.Buffer();
                request.body().writeTo(buffer);
                byte[] envelope = buffer.readByteArray();
                var process = json.readTree(Arrays.copyOfRange(envelope,5,envelope.length)).path("process");
                processes.add(process);
                if (ROOT.equals(process.path("cwd").asText()) && !exists.get())
                    return response(request,400,"cwd '/home/user/workspace' does not exist".getBytes(StandardCharsets.UTF_8));
                if (process.path("args").toString().contains("mkdir")) exists.set(true);
                byte[] end = "{\"event\":{\"end\":{\"exitCode\":0}}}".getBytes(StandardCharsets.UTF_8);
                return response(request,200,ByteBuffer.allocate(5+end.length).put((byte)0).putInt(end.length).put(end).array());
            }
            assertThat(request.url().host()).isEqualTo("platform.example.invalid");
            return response(request,200,"{\"sandboxID\":\"sandbox-test\",\"envdVersion\":\"0.4.0\",\"envdAccessToken\":\"fixture\"}".getBytes(StandardCharsets.UTF_8));
        }).build();
        var workspace=new WorkspaceSpec();workspace.setRoot(ROOT);
        var context = new ReleasedE2bFilesystemSpec().apiKey("fixture-key").apiBaseUrl("https://platform.example.invalid")
                .domain("example.invalid").templateId("fixture-template").workspaceSpec(workspace).workspaceRoot(ROOT).codec(E2bCodec.JSON).toSandboxContext();
        var options=(E2bSandboxClientOptions)context.getClientOptions();
        options.setHttpClient(http);
        @SuppressWarnings("unchecked")
        var client=(SandboxClient<E2bSandboxClientOptions>)context.getClient();
        Sandbox sandbox;
        if(resume) {
            var state=new E2bSandboxState();
            state.setSessionId("session");state.setTemplateId("fixture-template");state.setWorkspaceRoot(ROOT);
            state.setWorkspaceSpec(workspace);state.setCodec(E2bCodec.JSON);state.setPersistenceMode(E2bPersistenceMode.TAR);
            sandbox=client.resume(state);
        } else sandbox=client.create(context.getWorkspaceSpec(),context.getSnapshotSpec(),options);
        sandbox.start();
        assertThat(exists).isTrue();
        assertThat(sandbox.isRunning()).isTrue();
        assertThat(processes.getFirst().path("cwd").asText()).isEqualTo("/");
        assertThat(processes.getFirst().path("args").toString()).contains("mkdir",ROOT);
        assertThat(((E2bSandboxState)sandbox.getState()).getWorkspaceRoot()).isEqualTo(ROOT);
        sandbox.exec(RuntimeContext.empty(),"pwd",5);
        assertThat(processes.getLast().path("cwd").asText()).isEqualTo(ROOT);
    }

    private Response response(Request request,int status,byte[] body) {
        return new Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1).code(status).message("fixture")
                .body(ResponseBody.create(body,MediaType.get("application/connect+json"))).build();
    }
}
