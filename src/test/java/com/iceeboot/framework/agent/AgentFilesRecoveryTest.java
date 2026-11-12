package com.iceeboot.framework.agent;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.IAgentFileService;
import com.iceeboot.framework.service.agent.impl.AgentFileServiceImpl;
import com.iceeboot.framework.service.agent.runtime.*;
import io.agentscope.harness.agent.artifact.*;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
import io.agentscope.harness.agent.sandbox.Sandbox;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

class AgentFilesRecoveryTest {
    AgentFixture f;PublishedAgent p;IAgentFileService files;
    @BeforeEach void setup() throws Exception {
        f=new AgentFixture();
        var template=f.configs.saveCapability(new Capability(null,CapabilityKind.SANDBOX,"sandbox","sandbox","",true,null,null,null,new SandboxConfig("test-template",120)));
        p=f.publish(f.save("files",f.model().id(),List.of(),List.of(),List.of(),List.of(),true,template.id()));
        f.conversations().open(p.agent().id(),"1","session",p,"文件");files=f.files();
    }
    @Test void storesTwentyMibInSmallChunksAndReturnsIdenticalBytes() {
        byte[] bytes=new byte[(int)IAgentFileService.MAX_BYTES];new Random(7).nextBytes(bytes);
        var file=files.upload(p.agent().id(),"1","session","数据.bin",bytes);
        assertThat(f.jdbc.queryForObject("SELECT MAX(OCTET_LENGTH(payload)) FROM t_agent_file_chunk WHERE file_id=?",Integer.class,file.id())).isEqualTo(256*1024);
        assertThat(files.download(p.agent().id(),"1","session",file.id()).content()).isEqualTo(bytes);
        assertThat(file.sha256()).hasSize(64);
        assertThatThrownBy(() -> files.upload(p.agent().id(),"1","session","too-big",new byte[bytes.length+1])).hasMessage(ResponseMessage.Agent.FILE_INVALID);
    }
    @Test void rejectsPathNamesAndCrossAgentUserSessionAccessEvenWhenScopesExist() {
        byte[] bytes="private file".getBytes();
        for(String name:List.of("../secret","a/b","a\\b","header\r\nvalue",".."))
            assertThatThrownBy(() -> AgentFileServiceImpl.validate(name,bytes)).hasMessage(ResponseMessage.Agent.FILE_INVALID);
        var file=files.upload(p.agent().id(),"1","session","data.txt",bytes);
        f.conversations().open(p.agent().id(),"2","session",p,"other user");
        f.conversations().open(p.agent().id(),"1","other",p,"other session");
        f.conversations().open(p.agent().id(),"1","Session",p,"case-sensitive session");
        assertThatThrownBy(() -> files.download(p.agent().id(),"1","Session",file.id())).hasMessage(ResponseMessage.Agent.FILE_NOT_FOUND);
        f.conversations().open(999,"1","session",p,"other agent");
        assertThatThrownBy(() -> files.download(p.agent().id(),"2","session",file.id())).hasMessage(ResponseMessage.Agent.FILE_NOT_FOUND);
        assertThatThrownBy(() -> files.bind(p.agent().id(),"1","other","run",List.of(file.id()))).hasMessage(ResponseMessage.Agent.FILE_NOT_FOUND);
        assertThatThrownBy(() -> files.delete(999,"1","session",file.id())).hasMessage(ResponseMessage.Agent.FILE_NOT_FOUND);
        assertThat(files.list(p.agent().id(),"2","session","run")).isEmpty();
    }
    @Test void failedBindingRollsBackAndUsedFilesCannotBeDeleted() {
        var a=files.upload(p.agent().id(),"1","session","a",new byte[11*1024*1024]);
        var b=files.upload(p.agent().id(),"1","session","b",new byte[11*1024*1024]);
        assertThatThrownBy(() -> files.bind(p.agent().id(),"1","session","failed",List.of(a.id(),b.id()))).hasMessage(ResponseMessage.Agent.FILE_INVALID);
        assertThat(files.list(p.agent().id(),"1","session","failed")).isEmpty();
        files.bind(p.agent().id(),"1","session","used",List.of(a.id()));
        assertThatThrownBy(() -> files.delete(p.agent().id(),"1","session",a.id())).hasMessage(ResponseMessage.Agent.FILE_IN_USE);
        files.delete(p.agent().id(),"1","session",b.id());
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_file_chunk WHERE file_id=?",Integer.class,b.id())).isZero();
    }
    @Test void stagesBinaryAttachmentsUsingSafeArchiveNamesAndPersistsSdkArtifactDelivery() throws Exception {
        var file=files.upload(p.agent().id(),"1","session","你好.bin",new byte[]{0,1,2,3,(byte)255});
        var attached=files.bind(p.agent().id(),"1","session","run",List.of(file.id()));
        var runFiles=new RunFiles(files,p.agent().id(),"1","session","run",attached);
        var recorder=new RunRecorder(null,"",p,new Caller("1","token"),f.store,new AtomicInteger(10));
        var events=new ArrayList<AgentProgress>();recorder.observeProgress(events::add);
        Sandbox sandbox=mock(Sandbox.class);
        runFiles.stage(sandbox,recorder);
        var archive=ArgumentCaptor.forClass(InputStream.class);verify(sandbox).hydrateWorkspace(archive.capture());
        try(var tar=new TarArchiveInputStream(archive.getValue())) {
            assertThat(tar.getNextEntry().getName()).isEqualTo("attachments/"+file.id()+"/你好.bin");
            assertThat(tar.readAllBytes()).isEqualTo(attached.getFirst().content());
            assertThat(tar.getNextEntry()).isNull();
        }
        var delivered=runFiles.delivery(recorder).deliver(null,new ArtifactDeliveryRequest("/report.xlsx",new byte[]{7,8,9},"report.xlsx","report",false));
        assertThat(delivered.successful()).isTrue();
        var generated=files.list(p.agent().id(),"1","session","run").stream().filter(row -> row.kind()==AgentFileKind.ARTIFACT).findFirst().orElseThrow();
        assertThat(files.download(p.agent().id(),"1","session",generated.id()).content()).containsExactly(7,8,9);
        assertThat(events).anyMatch(e -> e.type()==AgentProgressType.ARTIFACT && e.callId().equals(generated.id()));
        assertThat(files.bind(p.agent().id(),"1","session","next-run",List.of(generated.id()))).hasSize(1);
    }
    @Test void retryCreatesFreshSessionUsingOriginalReleaseAndCopiesInputsWithoutPendingToolState() {
        var conversation=f.conversations().require(p.agent().id(),"1","session");
        f.conversations().begin(conversation,"previous","earlier question");
        f.conversations().finish(conversation,"previous","earlier answer","SUCCEEDED");f.conversations().release(conversation,"previous");
        f.conversations().begin(conversation,"failed","current task");
        var uploaded=files.upload(p.agent().id(),"1","session","data.txt","data".getBytes());
        files.bind(p.agent().id(),"1","session","failed",List.of(uploaded.id()));
        files.artifact(p.agent().id(),"1","session","failed","partial.txt","partial".getBytes());
        f.conversations().finish(conversation,"failed","partial model output","FAILED");f.conversations().release(conversation,"failed");
        f.configs.publish(p.agent().id(),"1");
        var retry=f.recovery().prepare(p.agent().id(),"1","session","failed");
        assertThat(retry.session().getReleaseId()).isEqualTo(p.releaseId());
        assertThat(retry.session().getSessionId()).isNotEqualTo("session");
        assertThat(retry.input()).contains("earlier question","earlier answer","current task").doesNotContain("partial model output");
        assertThat(retry.attachments()).hasSize(1);
        assertThat(retry.attachments().getFirst().id()).isNotEqualTo(uploaded.id());
        assertThat(f.conversations().messages(p.agent().id(),"1",retry.session().getSessionId(),Long.MAX_VALUE)).isEmpty();
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_session_state WHERE session_id=?",Integer.class,retry.session().getSessionId())).isZero();
        assertThat(files.download(p.agent().id(),"1",retry.session().getSessionId(),retry.attachments().getFirst().id()).content()).isEqualTo("data".getBytes());
    }
    @Test void artifactBoundaryDoesNotExposeStorageExceptionDetails() {
        var storage=mock(IAgentFileService.class);
        when(storage.artifact(anyLong(),anyString(),anyString(),anyString(),anyString(),any()))
                .thenThrow(new IllegalStateException("private-storage-detail"));
        var recorder=mock(RunRecorder.class);
        var result=new RunFiles(storage,1L,"1","session","run",List.of()).delivery(recorder)
                .deliver(null,new ArtifactDeliveryRequest("report","data".getBytes(),"report","",false));
        assertThat(result.successful()).isFalse();
        assertThat(result.error()).isEqualTo(ResponseMessage.Agent.FILE_STORAGE_FAILED).doesNotContain("private-storage-detail");
    }
    @Test void runningAndForeignTurnsCannotBeRecoveredButSucceededCanPrepareRegeneration() {
        var conversation=f.conversations().require(p.agent().id(),"1","session");
        f.conversations().begin(conversation,"running","task");
        assertThatThrownBy(() -> f.recovery().prepare(p.agent().id(),"1","session","running")).hasMessage(ResponseMessage.Agent.RECOVERY_NOT_ALLOWED);
        f.conversations().finish(conversation,"running","done","SUCCEEDED");f.conversations().release(conversation,"running");
        var regenerated = f.recovery().prepare(p.agent().id(),"1","session","running");
        assertThat(regenerated.input()).isEqualTo("task");
        assertThat(regenerated.session().getSessionId()).isNotEqualTo("session");
        assertThat(f.conversations().messages(p.agent().id(),"1",regenerated.session().getSessionId(),Long.MAX_VALUE)).isEmpty();
        f.conversations().open(p.agent().id(),"2","session",p,"other");
        assertThatThrownBy(() -> f.recovery().prepare(p.agent().id(),"2","session","running")).hasMessage(ResponseMessage.Agent.RECOVERY_NOT_ALLOWED);
    }
}
