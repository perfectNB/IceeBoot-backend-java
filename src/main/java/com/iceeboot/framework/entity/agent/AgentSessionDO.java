package com.iceeboot.framework.entity.agent;

import lombok.Data;

/** 数据库会话持久化记录。 */
@Data
public class AgentSessionDO {
    private Long agentId;
    private String userId;
    private String sessionId;
    private String releaseId;
    private Integer version;
    private String title;
    private long leaseUntil;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
}
