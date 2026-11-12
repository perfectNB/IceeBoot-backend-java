package com.iceeboot.framework.agent;

import com.iceeboot.framework.service.agent.runtime.ToolResultFileStore;
import io.agentscope.core.agent.*;
import io.agentscope.core.message.*;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.harness.agent.filesystem.remote.RemoteFilesystem;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.middleware.ToolResultEvictionMiddleware;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentToolResultFileTest {
    @Test void nativeEvictionKeepsFullEncryptedResultAvailableToNextRuntimeAndIsolatesSession() throws Exception {
        var f=new AgentFixture();
        var files=new RemoteFilesystem(new ToolResultFileStore(f.conversations().stateStore(1,"u","s")),List.of("tool-results"));
        var config=ToolResultEvictionConfig.builder().evictionPath(ToolResultFileStore.PATH).maxResultChars(1024).previewChars(256).build();
        var middleware=new ToolResultEvictionMiddleware(files,config);
        var agent=mock(Agent.class);when(agent.getName()).thenReturn("agent");
        String full="private-result-data-".repeat(1000);
        Msg result=ToolResultMessage.builder().result(new ToolResultBlock("call","execute",TextBlock.builder().text(full).build())).build();
        var input=new ReasoningInput(List.of(result),List.of(),GenerateOptions.builder().build());
        var ctx=RuntimeContext.builder().userId("u").sessionId("s").build();
        var candidate=middleware.prepareCandidate(agent,ctx,input,List.of());
        String preview=((TextBlock)candidate.input().messages().getFirst().getFirstContentBlock(ToolResultBlock.class).getOutput().getFirst()).getText();
        assertThat(preview).hasSizeLessThan(1200).contains("read_file");
        var path=java.util.regex.Pattern.compile("`([^`]+)`").matcher(preview);assertThat(path.find()).isTrue();
        var next=new RemoteFilesystem(new ToolResultFileStore(f.conversations().stateStore(1,"u","s")),List.of("tool-results"));
        assertThat(next.read(ctx,path.group(1),0,100000).fileData().content()).isEqualTo(full);
        for(var scoped:List.of(f.conversations().stateStore(2,"u","s"),f.conversations().stateStore(1,"other","s"),f.conversations().stateStore(1,"u","other"))) {
            assertThat(new RemoteFilesystem(new ToolResultFileStore(scoped),List.of("tool-results")).read(ctx,path.group(1),0,100000).isSuccess()).isFalse();
        }
        var payload=f.jdbc.queryForObject("SELECT payload FROM t_agent_session_state WHERE agent_id=1 AND user_id='u' AND session_id='s'",byte[].class);
        assertThat(new String(payload,java.nio.charset.StandardCharsets.UTF_8)).startsWith("enc:v2:").doesNotContain("private-result-data");
    }
    @Test void fileCasRejectsStaleUpdatesAndCanRecreateDeletedFile() throws Exception {
        var f=new AgentFixture();var store=new ToolResultFileStore(f.conversations().stateStore(1,"u","s"));var ns=List.of("tool-results");
        assertThat(store.putIfVersion(ns,"file",Map.of("content","old"),0)).isTrue();
        var saved=store.get(ns,"file");
        assertThat(store.putIfVersion(ns,"file",Map.of("content","stale"),0)).isFalse();
        assertThat(store.putIfVersion(ns,"file",Map.of("content","updated"),saved.version())).isTrue();
        store.delete(ns,"file");assertThat(store.get(ns,"file")).isNull();
        assertThat(store.putIfVersion(ns,"file",Map.of("content","recreated"),0)).isTrue();
        assertThat(store.get(ns,"file").value()).containsEntry("content","recreated");
    }
}
