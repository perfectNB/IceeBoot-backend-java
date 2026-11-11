package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.config.interceptor.PermissionInterceptor;
import com.iceeboot.framework.controller.agent.*;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.service.system.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

import java.util.*;

class AgentResourceApiTest {
    @Test
    void typedSkillCrudUsesDtoAndVoThroughRealPersistence() throws Exception {
        var f = new AgentFixture();
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        var mvc =
                MockMvcBuilders.standaloneSetup(new AgentSkillController(f.skillsService(), users, org.mockito.Mockito.mock(com.iceeboot.framework.service.agent.IAgentSkillImportService.class)))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            String response =
                    mvc.perform(
                                    post("/v1/agent-console/skills")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    "{\"code\":\"analyze\",\"name\":\"分析\",\"enabled\":true,\"content\":\"version-one\",\"resources\":{\"sample.txt\":\"data\"},\"toolIds\":[]}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.result.content").value("version-one"))
                            .andExpect(jsonPath("$.result.resources['sample.txt']").value("data"))
                            .andExpect(jsonPath("$.result.configJson").doesNotExist())
                            .andReturn()
                            .getResponse()
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            long id = f.json.readTree(response).path("result").path("id").asLong();
            mvc.perform(
                            put("/v1/agent-console/skills")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"id\":"
                                                    + id
                                                    + ",\"code\":\"analyze\",\"name\":\"分析\",\"enabled\":true,\"content\":\"version-two\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content").value("version-two"));
            mvc.perform(get("/v1/agent-console/skills/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.content").value("version-two"));
            mvc.perform(delete("/v1/agent-console/skills/" + id)).andExpect(status().isOk());
            mvc.perform(get("/v1/agent-console/skills/" + id)).andExpect(status().isBadRequest());
        }
    }

    @Test
    void typedMcpResponseMasksHeadersAndRejectsCrossTypeUpdates() throws Exception {
        var f = new AgentFixture();
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        var mvc =
                MockMvcBuilders.standaloneSetup(new AgentMcpController(f.mcpsService(), users))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        var tool = f.http("http_tool", "https://example.invalid/data");
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            mvc.perform(
                            post("/v1/agent-console/mcps")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"code\":\"remote\",\"name\":\"MCP\",\"enabled\":true,\"url\":\"https://example.invalid/mcp\",\"headers\":{\"Authorization\":\"secret\"},\"allowedTools\":[\"search\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.headers.Authorization").value("********"));
            mvc.perform(
                            put("/v1/agent-console/mcps")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"id\":"
                                                    + tool.id()
                                                    + ",\"code\":\"http_tool\",\"name\":\"MCP\",\"url\":\"https://example.invalid/mcp\",\"allowedTools\":[\"search\"]}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(
                            put("/v1/agent-console/mcps")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"code\":\"remote\",\"name\":\"MCP\",\"url\":\"https://example.invalid/mcp\",\"allowedTools\":[\"search\"]}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void everyControllerUsesDtoVoAndDynamicPermissions() {
        for (var controller :
                List.of(
                        AgentConsoleController.class,
                        AgentSkillController.class,
                        AgentToolController.class,
                        AgentMcpController.class,
                        AgentSandboxTemplateController.class,
                        AgentKnowledgeController.class)) {
            for (var method : controller.getDeclaredMethods()) {
                if (method.getAnnotations().length == 0
                        || Arrays.stream(method.getAnnotations())
                                .noneMatch(
                                        a ->
                                                Set.of(
                                                                GetMapping.class,
                                                                PostMapping.class,
                                                                PutMapping.class,
                                                                DeleteMapping.class)
                                                        .contains(a.annotationType()))) continue;
                var permission = method.getAnnotation(RequirePermission.class);
                assertThat(permission).as(method.toString()).isNotNull();
                assertThat(permission.dynamic()).isTrue();
                assertThat(permission.code()).isBlank();
                assertThat(method.getGenericReturnType().getTypeName())
                        .doesNotContain("framework.model", "framework.entity");
                for (var parameter : method.getParameters())
                    if (parameter.isAnnotationPresent(RequestBody.class)) {
                        assertThat(parameter.getType().getPackageName())
                                .startsWith("com.iceeboot.framework.dto.");
                    }
            }
        }
    }

    @Test
    void typedEndpointRejectsMissingPermissionBeforeCallingServices() throws Exception {
        var service = mock(com.iceeboot.framework.service.agent.IAgentSandboxTemplateService.class);
        var users = mock(ISysUserService.class);
        var cache = mock(IAuthCacheService.class);
        when(cache.hasUserPermissions(eq(7L), anyList(), anyBoolean())).thenReturn(false);
        var mvc =
                MockMvcBuilders.standaloneSetup(new AgentSandboxTemplateController(service, users))
                        .addInterceptors(new PermissionInterceptor(cache))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get("/v1/agent-console/sandbox-templates"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service, users);
        }
    }

    @Test
    void existingConsolePayloadsStillCreateModelsCapabilitiesAgentsAndReleases() throws Exception {
        var f = new AgentFixture();
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        var mvc =
                MockMvcBuilders.standaloneSetup(
                                new AgentConsoleController(
                                        f.configs,
                                        mock(
                                                com.iceeboot.framework.service.agent
                                                        .IAgentExecutionService.class),
                                        f.store,
                                        users,
                                        new AgentApiConverter(f.json), mock(com.iceeboot.framework.service.agent.IAgentConversationService.class)))
                        .setControllerAdvice(new AgentConsoleExceptionHandler())
                        .build();
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(1L);
            String modelResponse =
                    mvc.perform(
                                    post("/v1/agent-console/models")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    "{\"name\":\"test\",\"provider\":\"GPT\",\"protocol\":\"OPENAI_CHAT\",\"baseUrl\":\"https://example.invalid/v1\",\"apiKey\":\"secret\",\"model\":\"test-model\",\"enabled\":true}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.result.apiKey").value("********"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            long modelId = f.json.readTree(modelResponse).path("result").path("id").asLong();
            String capabilityResponse =
                    mvc.perform(
                                    post("/v1/agent-console/capabilities")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    "{\"kind\":\"SKILL\",\"code\":\"legacy\",\"name\":\"Skill\",\"enabled\":true,\"skill\":{\"content\":\"analyze"
                                                        + " data\",\"resources\":{},\"toolIds\":[]}}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.result.skill.content").value("analyze data"))
                            .andExpect(jsonPath("$.result.kind").value("SKILL"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            long skillId = f.json.readTree(capabilityResponse).path("result").path("id").asLong();
            String agentResponse =
                    mvc.perform(
                                    post("/v1/agent-console/agents")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(
                                                    "{\"code\":\"analyst\",\"name\":\"Analyst\",\"prompt\":\"analyze\",\"modelConfigId\":"
                                                            + modelId
                                                            + ",\"maxRounds\":3,\"enabled\":true,\"outputFormat\":\"TEXT\",\"skillIds\":["
                                                            + skillId
                                                            + "]}"))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.result.draft.code").value("analyst"))
                            .andReturn()
                            .getResponse()
                            .getContentAsString();
            long agentId =
                    f.json.readTree(agentResponse).path("result").path("draft").path("id").asLong();
            mvc.perform(post("/v1/agent-console/agents/" + agentId + "/publish"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.version").value(1));
            assertThat(f.store.agent(agentId).activeVersion()).isEqualTo(1);
            assertThat(f.store.model(modelId).apiKey()).startsWith("enc:v2:primary:");
        }
    }
}
