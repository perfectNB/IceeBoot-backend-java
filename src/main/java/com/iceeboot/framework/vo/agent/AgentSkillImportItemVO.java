package com.iceeboot.framework.vo.agent;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;
import java.util.Map;
@Data
@Schema(description = "待导入技能及其来源目录")
public class AgentSkillImportItemVO {
    private String sourcePath;
    private String code;
    private String name;
    private String description;
    private boolean enabled;
    private String content;
    private Map<String,String> resources;
    private List<Long> toolIds;
}
