package com.iceeboot.framework.service.agent;
import com.iceeboot.framework.dto.create.agent.*;
import com.iceeboot.framework.vo.agent.*;
import java.util.List;
public interface IAgentSkillImportService {
    AgentSkillImportPreviewVO previewArchive(AgentSkillArchiveDTO request);
    AgentSkillImportPreviewVO previewGithub(AgentSkillGithubDTO request);
    List<AgentSkillVO> commit(AgentSkillImportDTO request);
}
