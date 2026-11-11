package com.iceeboot.framework.agent;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.controller.agent.AgentConsoleController;
import com.iceeboot.framework.service.agent.IAgentExecutionService;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.service.system.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

class AgentConversationApiTest {
    @Test
    void historyUsesLoginIdentityAndNeverAcceptsAnotherUsersIdentity() throws Exception {
        var f = new AgentFixture();
        var model = f.model();
        var agent = f.save("api_chat", model.id(), List.of(), List.of(), List.of(), List.of(), false, null);
        f.publish(agent);
        long id = agent.draft().id();
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(7L)).thenReturn(List.of("superAdmin"));
        when(users.getUserRoles(8L)).thenReturn(List.of("superAdmin"));
        var conversations = f.conversations();
        var session = conversations.open(id, "7", "owned_session",
                f.store.release(f.store.agent(id).activeReleaseId()), "my history");
        conversations.begin(session, "api_run", "private question");
        conversations.finish(session, "api_run", "private answer", "SUCCEEDED");
        conversations.release(session, "api_run");
        var mvc = MockMvcBuilders.standaloneSetup(new AgentConsoleController(f.configs,
                mock(IAgentExecutionService.class), f.store, users, new AgentApiConverter(f.json), conversations))
                .setControllerAdvice(new AgentConsoleExceptionHandler()).build();
        String root = "/v1/agent-console/agents/" + id + "/sessions";
        try (var tokens = mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            mvc.perform(get(root)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].sessionId").value("owned_session"));
            mvc.perform(get(root + "/owned_session/messages")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.result[0].content").value("private question"))
                    .andExpect(jsonPath("$.result[1].content").value("private answer"));
            mvc.perform(post(root).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"new thread\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.title").value("new thread"));
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(8L);
            mvc.perform(get(root).param("userId", "7")).andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").isEmpty());
            mvc.perform(get(root + "/owned_session/messages").param("userId", "7"))
                    .andExpect(status().isNotFound());
        }
    }
}
