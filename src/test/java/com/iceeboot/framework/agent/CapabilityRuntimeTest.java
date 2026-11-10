package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.runtime.*;
import com.iceeboot.framework.service.system.ISysUserService;
import com.sun.net.httpserver.HttpServer;

import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.core.tool.*;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.modelcontextprotocol.spec.McpSchema;

import org.junit.jupiter.api.*;

import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.*;

class CapabilityRuntimeTest {
    @Test
    void projectHttpToolInjectsIdentityOutsideModelAndRechecksPermission() throws Exception {
        AgentFixture f = new AgentFixture();
        var users = mock(ISysUserService.class);
        when(users.getUserPermissions(7L)).thenReturn(List.of("orders:read"));
        AtomicReference<String> token = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/orders",
                e -> {
                    token.set(e.getRequestHeaders().getFirst("Authorization"));
                    byte[] bytes = "orders".getBytes();
                    e.sendResponseHeaders(200, bytes.length);
                    e.getResponseBody().write(bytes);
                    e.close();
                });
        server.start();
        try {
            f.properties.setProjectApiBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            var c =
                    new Capability(
                            1L,
                            CapabilityKind.TOOL,
                            "orders",
                            "订单",
                            "查询订单",
                            true,
                            null,
                            new ToolConfig(
                                    ToolKind.HTTP,
                                    "/v1/orders",
                                    "GET",
                                    Map.of(),
                                    Map.of("type", "object"),
                                    null,
                                    true,
                                    "orders:read"),
                            null,
                            null);
            var tool =
                    new ConfiguredTool(
                            c,
                            new Caller("7", "caller-token"),
                            f.properties,
                            f.secrets,
                            f.store,
                            users,
                            false);
            var call =
                    ToolCallParam.builder()
                            .toolUseBlock(
                                    ToolUseBlock.builder()
                                            .id("call")
                                            .name("orders")
                                            .input(Map.of())
                                            .build())
                            .input(Map.of())
                            .build();
            assertThat(tool.callAsync(call).block().getState()).isEqualTo(ToolResultState.SUCCESS);
            assertThat(token.get()).isEqualTo("Bearer caller-token");
            assertThat(tool.getParameters().toString())
                    .doesNotContain("caller-token", "Authorization");
            when(users.getUserPermissions(7L)).thenReturn(List.of());
            token.set(null);
            assertThat(tool.callAsync(call).block().getState()).isEqualTo(ToolResultState.ERROR);
            assertThat(token.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void mcpRegistrationExposesOnlyAllowlistedToolsWithServicePrefix() {
        McpClientWrapper client = mock(McpClientWrapper.class);
        when(client.getName()).thenReturn("docs");
        when(client.isInitialized()).thenReturn(true);
        when(client.initialize()).thenReturn(Mono.empty());
        McpSchema.Tool allowed =
                McpSchema.Tool.builder()
                        .name("search")
                        .description("搜索")
                        .inputSchema(
                                new McpSchema.JsonSchema(
                                        "object", Map.of(), List.of(), false, null, null))
                        .build();
        McpSchema.Tool forbidden =
                McpSchema.Tool.builder()
                        .name("delete_all")
                        .description("删除全部")
                        .inputSchema(
                                new McpSchema.JsonSchema(
                                        "object", Map.of(), List.of(), false, null, null))
                        .build();
        when(client.listTools()).thenReturn(Mono.just(List.of(allowed, forbidden)));
        when(client.getCachedTool("search")).thenReturn(allowed);
        when(client.getCachedTool("delete_all")).thenReturn(forbidden);
        Toolkit toolkit = new Toolkit();
        toolkit.registration()
                .mcpClient(client)
                .mcpToolNamePrefix("docs__")
                .enableTools(List.of("search"))
                .apply();
        assertThat(toolkit.getToolNames())
                .contains("docs__search")
                .doesNotContain("delete_all", "docs__delete_all");
    }

    @Test
    void harnessLoadsOnlyPublishedSkillAndItsResourceOnDemand() throws Exception {
        AgentFixture f = new AgentFixture();
        var m = f.model();
        var linkedTool = f.http("lookup", "https://example.invalid/data");
        var skill =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.SKILL,
                                "analysis",
                                "分析",
                                "读取分析说明",
                                true,
                                new SkillConfig(
                                        "技能正文：先读取说明再分析",
                                        Map.of("references/guide.md", "资源正文"),
                                        List.of(linkedTool.id())),
                                null,
                                null,
                                null));
        f.publish(
                f.save(
                        "agent",
                        m.id(),
                        List.of(skill.id()),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        null));
        var models = mock(AgentModelFactory.class);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        AtomicInteger step = new AtomicInteger();
        when(models.create(any(), any()))
                .thenReturn(
                        HarnessExecutionTest.model(
                                (messages, tools) -> {
                                    int round = step.getAndIncrement();
                                    if (round == 0) {
                                        assertThat(tools)
                                                .extracting(ToolSchema::getName)
                                                .contains("lookup");
                                        var viewer =
                                                tools.stream()
                                                        .filter(
                                                                t ->
                                                                        t.getName()
                                                                                .equals(
                                                                                        "load_skill_through_path"))
                                                        .findFirst()
                                                        .orElseThrow();
                                        String skillId =
                                                new SnapshotSkillRepository(List.of(skill))
                                                        .getAllSkills()
                                                        .getFirst()
                                                        .getSkillId();
                                        return HarnessExecutionTest.tool(
                                                viewer.getName(),
                                                Map.of("skillId", skillId, "path", "SKILL.md"));
                                    }
                                    assertThat(f.store.json(messages)).contains("技能正文");
                                    assertThat(tools)
                                            .extracting(ToolSchema::getName)
                                            .contains("lookup");
                                    return HarnessExecutionTest.text("已使用技能", 5, 2);
                                }));
        var factory =
                new HarnessRuntimeFactory(
                        models, f.properties, f.secrets, f.store, mock(ISysUserService.class));
        var service =
                new com.iceeboot.framework.service.agent.impl.AgentExecutionServiceImpl(
                        f.store, factory, models, f.properties, f.json);
        assertThat(
                        service.invoke(
                                        "agent",
                                        new InvokeRequest("分析", null),
                                        new Caller("1", "token"))
                                .output())
                .isEqualTo("已使用技能");
    }

    @Test
    void scriptToolResolvesExecuteFromCurrentHarnessToolkitRatherThanOriginalBuilder() {
        var properties = new AgentProperties();
        var secrets = new com.iceeboot.framework.service.agent.security.AgentSecrets(properties);
        var store = mock(com.iceeboot.framework.service.agent.IAgentPersistenceService.class);
        when(store.json(any())).thenReturn("{\"text\":\"a'b\"}");
        var runtimeToolkit = mock(Toolkit.class);
        var executor = mock(AgentTool.class);
        when(runtimeToolkit.getTool("execute")).thenReturn(executor);
        when(executor.callAsync(any()))
                .thenReturn(
                        Mono.just(
                                ToolResultBlock.text("sandbox-only")
                                        .withState(ToolResultState.SUCCESS)));
        var agent = mock(io.agentscope.core.agent.Agent.class);
        when(agent.getToolkit()).thenReturn(runtimeToolkit);
        var c =
                new Capability(
                        1L,
                        CapabilityKind.TOOL,
                        "script",
                        "脚本",
                        "运行脚本",
                        true,
                        null,
                        new ToolConfig(
                                ToolKind.SCRIPT,
                                null,
                                null,
                                Map.of(),
                                Map.of("type", "object"),
                                "python3 -c 'print(1)'",
                                false,
                                null),
                        null,
                        null);
        var tool =
                new ConfiguredTool(
                        c,
                        new Caller("1", "token"),
                        properties,
                        secrets,
                        store,
                        mock(ISysUserService.class),
                        true);
        var param =
                ToolCallParam.builder()
                        .toolUseBlock(
                                ToolUseBlock.builder()
                                        .id("script-call")
                                        .name("script")
                                        .input(Map.of())
                                        .build())
                        .input(Map.of())
                        .agent(agent)
                        .build();
        assertThat(tool.callAsync(param).block().getState()).isEqualTo(ToolResultState.SUCCESS);
        verify(runtimeToolkit).getTool("execute");
        var captured = org.mockito.ArgumentCaptor.forClass(ToolCallParam.class);
        verify(executor).callAsync(captured.capture());
        assertThat(captured.getValue().getAgent()).isSameAs(agent);
        assertThat(captured.getValue().getInput()).containsKey("command");
    }
}
