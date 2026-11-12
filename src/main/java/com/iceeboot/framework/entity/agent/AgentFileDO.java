package com.iceeboot.framework.entity.agent;
import com.iceeboot.framework.enums.agent.AgentFileKind;
import lombok.Data;
@Data
public class AgentFileDO {
    private String id;
    private Long agentId;
    private String userId;
    private String sessionId;
    private AgentFileKind kind;
    private String name;
    private long sizeBytes;
    private String sha256;
    private java.time.LocalDateTime createTime;
}
