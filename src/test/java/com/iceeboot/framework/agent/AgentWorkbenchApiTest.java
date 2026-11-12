package com.iceeboot.framework.agent;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.controller.agent.AgentWorkbenchController;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.impl.AgentWorkbenchServiceImpl;
import com.iceeboot.framework.service.agent.convert.AgentApiConverter;
import com.iceeboot.framework.service.agent.runtime.AgentRunControl;
import com.iceeboot.framework.service.system.ISysUserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;

class AgentWorkbenchApiTest {
    @Test void uploadAndBinaryDownloadUseLoginIdentityWithPrivateHeadersAndNoCrossUserAccess() throws Exception {
        var f=new AgentFixture();
        var sandbox=f.configs.saveCapability(new Capability(null,CapabilityKind.SANDBOX,"sandbox","sandbox","",true,null,null,null,new SandboxConfig("template",120)));
        var agent=f.publish(f.save("api_files",f.model().id(),List.of(),List.of(),List.of(),List.of(),true,sandbox.id()));
        long id=agent.agent().id();
        f.conversations().open(id,"7","session",agent,"files");
        f.conversations().open(id,"8","session",agent,"other");
        var users=mock(ISysUserService.class);
        when(users.getUserRoles(7L)).thenReturn(List.of("superAdmin"));
        when(users.getUserRoles(8L)).thenReturn(List.of("superAdmin"));
        var mvc=MockMvcBuilders.standaloneSetup(new AgentWorkbenchController(
                new AgentWorkbenchServiceImpl(f.configs,f.store,f.properties,f.json,mock(AgentRunControl.class)),
                f.configs,f.files(),f.recovery(),users,new AgentApiConverter(f.json)))
                .setControllerAdvice(new AgentConsoleExceptionHandler()).build();
        String root="/v1/agent-console/agents/"+id+"/sessions/session/files";
        byte[] payload=new byte[]{0,1,2,(byte)255};
        try(var tokens=mockStatic(TokenUtils.class)) {
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(7L);
            var response=mvc.perform(multipart(root).file(new MockMultipartFile("file","数据.bin","application/octet-stream",payload)).param("userId","8"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.result.name").value("数据.bin"))
                    .andReturn().getResponse();
            String file=f.json.readTree(response.getContentAsString()).path("result").path("id").asText();
            var download=mvc.perform(get(root+"/"+file)).andExpect(status().isOk())
                    .andExpect(content().contentType("application/octet-stream"))
                    .andExpect(header().string("Cache-Control","no-store"))
                    .andExpect(header().string("X-Content-Type-Options","nosniff"))
                    .andReturn().getResponse();
            assertThat(download.getContentAsByteArray()).isEqualTo(payload);
            assertThat(download.getHeader("Content-Disposition")).startsWith("attachment;").contains("UTF-8");
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(8L);
            mvc.perform(get(root+"/"+file).param("userId","7")).andExpect(status().isNotFound());
            mvc.perform(delete(root+"/"+file).param("userId","7")).andExpect(status().isNotFound());
            tokens.when(TokenUtils::getCurrentUserId).thenReturn(9L);
            mvc.perform(get(root).param("runId","run")).andExpect(status().isForbidden());
        }
    }
}
