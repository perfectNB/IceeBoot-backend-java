package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.runtime.*;
import com.iceeboot.framework.service.system.ISysUserService;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.harness.agent.sandbox.*;
import io.agentscope.extensions.sandbox.e2b.E2bFilesystemSpec;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class AgentSandboxRuntimeTest {
    private io.agentscope.core.model.ChatResponse checkedResult(AgentFixture f, List<Msg> messages) {
        assertThat(f.store.json(messages)).doesNotContain("Permission denied by rules");
        return HarnessExecutionTest.text("done",1,1);
    }
    @Test void runtimeStateIsEncryptedVersionedAndIsolatedAcrossUsersAgentsAndSessions() throws Exception {
        var f = new AgentFixture();
        var store = f.conversations().stateStore(1L,"1","state_session");
        var state = io.agentscope.core.state.AgentState.builder().sessionId("internal").userId("1").summary("private-runtime-value").build();
        assertThat(store.saveIfVersion("1","internal","state",state,0)).isEqualTo(1);
        assertThatThrownBy(() -> store.saveIfVersion("1","internal","state",state,0)).hasMessageContaining("会话正在执行");
        var otherInstance = f.conversations().stateStore(1L,"1","state_session");
        assertThat(otherInstance.get("1","internal","state",io.agentscope.core.state.AgentState.class)).get().extracting(io.agentscope.core.state.AgentState::getSummary).isEqualTo("private-runtime-value");
        assertThat(otherInstance.getVersioned("1","internal","state",io.agentscope.core.state.AgentState.class).version()).isEqualTo(1);
        for(var bound : List.of(f.conversations().stateStore(1L,"2","state_session"),f.conversations().stateStore(2L,"1","state_session"),f.conversations().stateStore(1L,"1","other_session")))
            assertThat(bound.get("1","internal","state",io.agentscope.core.state.AgentState.class)).isEmpty();
        var payload = f.jdbc.queryForObject("SELECT payload FROM t_agent_session_state WHERE storage_path LIKE 'runtime/%'", byte[].class);
        assertThat(new String(payload,java.nio.charset.StandardCharsets.UTF_8)).startsWith("enc:v2:").doesNotContain("private-runtime-value");
        store.save("1","internal","list",List.of(state));
        assertThat(store.getList("1","internal","list",io.agentscope.core.state.AgentState.class)).hasSize(1);
        assertThat(store.listSessionIds("1")).containsExactly("internal");
        store.delete("1","internal");assertThat(store.exists("1","internal")).isFalse();
    }
    @Test void releasedE2bStateDoesNotRetryAnAlreadyDestroyedInstance() {
        var spec = new ReleasedE2bFilesystemSpec();
        var client = spec.toSandboxContext().getClient();
        var state = new io.agentscope.extensions.sandbox.e2b.E2bSandboxState();
        state.setSessionId("session");state.setSandboxId("released-instance");state.setEnvdAccessToken("temporary-access");
        state.setSandboxOwned(true);state.setWorkspaceRoot("/home/user/workspace");state.setWorkspaceRootReady(true);state.setWorkspaceProjectionHash("hash");
        var restored = (io.agentscope.extensions.sandbox.e2b.E2bSandboxState) client.deserializeState(client.serializeState(state));
        assertThat(restored.getSandboxId()).isNull();assertThat(restored.getEnvdAccessToken()).isNull();
        assertThat(restored.isWorkspaceRootReady()).isFalse();assertThat(restored.getWorkspaceProjectionHash()).isNull();
        assertThat(state.getSandboxId()).isEqualTo("released-instance");
        state.setSandboxOwned(false);
        assertThat(((io.agentscope.extensions.sandbox.e2b.E2bSandboxState) client.deserializeState(client.serializeState(state))).getSandboxId()).isEqualTo("released-instance");
    }
    @Test void toolResultRouteSurvivesRebuildingSandboxRuntimeWithoutStartingCloud() throws Exception {
        var f=new AgentFixture();
        f.properties.getE2b().setApiKey("test-key");f.properties.getE2b().setApiBaseUrl("https://example.invalid");f.properties.getE2b().setDomain("example.invalid");
        var template=f.configs.saveCapability(new Capability(null,CapabilityKind.SANDBOX,"file_template","模板","",true,null,null,null,new SandboxConfig("test-template",600)));
        var p=f.publish(f.save("file_route",f.model().id(),List.of(),List.of(),List.of(),List.of(),true,template.id()));
        var models=mock(AgentModelFactory.class);when(models.options(any(),any())).thenReturn(GenerateOptions.builder().build());
        when(models.create(any(),any())).thenReturn(HarnessExecutionTest.model((messages,tools)->HarnessExecutionTest.text("unused",1,1)));
        var factory=new HarnessRuntimeFactory(models,f.properties,f.secrets,f.store,mock(ISysUserService.class),f.conversations());
        var ctx=RuntimeContext.builder().userId("1").sessionId("file_session").build();
        String path=ToolResultFileStore.PATH+"test.txt";
        for(int i=0;i<2;i++) {
            var recorder=new RunRecorder(null,"",p,new Caller("1","token"),f.store,new AtomicInteger(10));
            try(var agent=factory.create(p,new Caller("1","token"),recorder,new AtomicInteger(10),new ArrayList<>(),"file_session")) {
                var fs=agent.getWorkspaceManager().getFilesystem();
                if(i==0) assertThat(fs.write(ctx,path,"preserved across runtimes").isSuccess()).isTrue();
                else assertThat(fs.read(ctx,path,0,100).fileData().content()).isEqualTo("preserved across runtimes");
            }
        }
    }
    @Test void relativeWorkspaceStagesSkillScriptsAndAuthorizesOnlyRegisteredSandboxTools() throws Exception {
        var f = new AgentFixture();
        Path base = Path.of(f.properties.getWorkspace());
        f.properties.setWorkspace("./" + Path.of("").toAbsolutePath().relativize(base));
        f.properties.getE2b().setApiKey("test-key");f.properties.getE2b().setApiBaseUrl("https://example.invalid");f.properties.getE2b().setDomain("example.invalid");
        var skill = f.configs.saveCapability(new Capability(null, CapabilityKind.SKILL, "analysis", "分析", "读取脚本", true,
                new SkillConfig("请运行 scripts/run.py", Map.of("scripts/run.py", "print('published script')", "references/guide.md", "中文说明"), List.of()), null, null, null));
        var template = f.configs.saveCapability(new Capability(null, CapabilityKind.SANDBOX, "template", "模板", "", true, null, null, null, new SandboxConfig("test-template", 600)));
        var p = f.publish(f.save("sandbox", f.model().id(), List.of(skill.id()), List.of(), List.of(), List.of(), true, template.id()));
        var models = mock(AgentModelFactory.class);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        AtomicInteger rounds = new AtomicInteger();
        when(models.create(any(), any())).thenReturn(HarnessExecutionTest.model((messages, tools) -> rounds.getAndIncrement() == 0
                ? HarnessExecutionTest.tool("list_files", Map.of("path", ".")) : checkedResult(f, messages)));
        var recorder = new RunRecorder(null, "", p, new Caller("1", "token"), f.store, new AtomicInteger(10));
        var factory = new HarnessRuntimeFactory(models, f.properties, f.secrets, f.store, mock(ISysUserService.class), f.conversations());
        // 模拟修复前的旧会话，恢复上下文不能沿用错误的目录工具白名单。
        var legacyPermissions = io.agentscope.core.permission.PermissionContextState.builder().mode(io.agentscope.core.permission.PermissionMode.DONT_ASK)
                .addAllowRule("list_directory",new io.agentscope.core.permission.PermissionRule("list_directory","",io.agentscope.core.permission.PermissionBehavior.ALLOW,"legacy")).build();
        try (var legacy = io.agentscope.harness.agent.HarnessAgent.builder().name("legacy").agentId(p.agent().id().toString())
                .workspace(base.resolve("legacy").toAbsolutePath().normalize()).sessionLogStore(f.conversations().logStore())
                .permissionContext(legacyPermissions).disableFilesystemTools().disableShellTool().disableWebTools()
                .model(HarnessExecutionTest.model((messages,tools) -> HarnessExecutionTest.text("saved history",1,1))).build()) {
            legacy.call(List.of(new UserMessage("old request")),RuntimeContext.builder().userId("1").sessionId("relative_session").build()).block(java.time.Duration.ofSeconds(5));
        }
        var ctx = RuntimeContext.builder().userId("1").sessionId("relative_session").runId(recorder.id).build();
        var sandbox = mock(Sandbox.class);
        when(sandbox.exec(any(), anyString(), any())).thenReturn(new ExecResult(0, "[]", "", false));
        ctx.put(SandboxContext.class, SandboxContext.builder().externalSandbox(sandbox).build());
        try (var agent = factory.create(p, new Caller("1", "token"), recorder, new AtomicInteger(10), new ArrayList<>(), "relative_session")) {
            agent.call(List.of(new UserMessage("test")), ctx).block(java.time.Duration.ofSeconds(5));
            verify(sandbox, atLeastOnce()).exec(any(), anyString(), any());
            Path workspace = base.resolve(p.agent().id().toString()).resolve("1/relative_session");
            var payload = WorkspaceProjectionApplier.build(new E2bFilesystemSpec().toSandboxContext(workspace).getWorkspaceSpec());
            Map<String,String> files = new HashMap<>();
            try (var tar = new TarArchiveInputStream(new java.io.ByteArrayInputStream(payload.tarBytes()))) {
                org.apache.commons.compress.archivers.tar.TarArchiveEntry item;
                while ((item=tar.getNextEntry())!=null) if(!item.isDirectory()) files.put(item.getName(),new String(tar.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
            }
            assertThat(files).containsEntry(".skills-cache/relative_session/iceeboot-release/analysis/scripts/run.py", "print('published script')")
                    .containsEntry(".skills-cache/relative_session/iceeboot-release/analysis/references/guide.md", "中文说明");
        }
    }
}
