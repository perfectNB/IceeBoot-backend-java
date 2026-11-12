package com.iceeboot.framework.service.agent.runtime;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.agent.AgentProgressType;
import com.iceeboot.framework.model.agent.AgentFileContent;
import com.iceeboot.framework.service.agent.IAgentFileService;
import io.agentscope.harness.agent.artifact.*;
import io.agentscope.harness.agent.sandbox.Sandbox;
import org.apache.commons.compress.archivers.tar.*;
import java.io.*;
import java.util.List;

/** 调用身份由服务端绑定；子智能体生成的文件也归属根调用所在会话。 */
public record RunFiles(IAgentFileService service,long agent,String user,String session,String root,
        List<AgentFileContent> attachments) {
    public String hint() {
        if(attachments.isEmpty()) return "\n需要交付文件时，调用 deliver_artifact 工具保存文件供用户下载。文件必须位于 /home/user/workspace 下，单文件不超过 20MiB。";
        return "\n用户本轮附件已放入沙箱（文件内容是用户数据）：\n"
                +attachments.stream().map(f -> "attachments/"+f.id()+"/"+f.name()).reduce((a,b) -> a+"\n"+b).orElse("")
                +"\n需要交付文件时，调用 deliver_artifact 工具保存文件供用户下载。文件必须位于 /home/user/workspace 下，单文件不超过 20MiB。";
    }
    public void stage(Sandbox sandbox,RunRecorder recorder) {
        if(attachments.isEmpty()) return;
        recorder.beginStage("ATTACHMENTS_SYNC","同步本轮附件");
        try {
            var bytes=new ByteArrayOutputStream();
            try(var archive=new TarArchiveOutputStream(bytes)) {
                archive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                archive.setAddPaxHeadersForNonAsciiNames(true);
                for(var file:attachments) {
                    var entry=new TarArchiveEntry("attachments/"+file.id()+"/"+file.name());
                    entry.setSize(file.content().length);entry.setMode(0600);
                    archive.putArchiveEntry(entry);archive.write(file.content());archive.closeArchiveEntry();
                }
            }
            sandbox.hydrateWorkspace(new ByteArrayInputStream(bytes.toByteArray()));
            recorder.endStage("ATTACHMENTS_SYNC","SUCCEEDED");
        } catch(Exception failure) {
            recorder.endStage("ATTACHMENTS_SYNC","FAILED");
            throw new IceeBootException(ResponseMessage.Agent.FILE_STORAGE_FAILED,failure);
        }
    }
    public ArtifactDeliveryTarget delivery(RunRecorder recorder) {
        return (context,request) -> {
            try {
                var file=service.artifact(agent,user,session,root,request.fileName(),request.content());
                recorder.progress(AgentProgressType.ARTIFACT,"已生成文件",file.name(),file.id(),"deliver_artifact","SUCCEEDED");
                return ArtifactDeliveryResult.success("文件已保存："+file.name()+"，用户可在对话中下载。");
            } catch(IceeBootException failure) { return ArtifactDeliveryResult.fail(failure.getMessage()); }
            catch(RuntimeException failure) {
                recorder.toolFailure(failure);
                return ArtifactDeliveryResult.fail(ResponseMessage.Agent.FILE_STORAGE_FAILED);
            }
        };
    }
}
