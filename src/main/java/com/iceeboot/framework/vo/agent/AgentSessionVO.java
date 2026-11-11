package com.iceeboot.framework.vo.agent;

import lombok.Data;

/** 数据库会话持久化记录。 */
@Data
public class AgentSessionVO {
    private Long agentId;
    private String sessionId;
    private String releaseId;
    private Integer version;
    private String title;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
}
