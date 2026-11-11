package com.iceeboot.framework.vo.agent;

import lombok.Data;

/** 数据库会话持久化记录。 */
@Data
public class AgentMessageVO {
    private Long id;
    private Long agentId;
    private String userId;
    private String sessionId;
    private String runId;
    private String role;
    private String content;
    private String status;
    private java.time.LocalDateTime createTime;
}
