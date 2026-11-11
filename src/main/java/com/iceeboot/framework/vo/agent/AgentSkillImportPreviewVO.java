package com.iceeboot.framework.vo.agent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
@Data
@Schema(description = "技能导入预览，确认前不写入数据库")
public class AgentSkillImportPreviewVO {
    private List<AgentSkillImportItemVO> skills;
    private List<String> warnings;
}
