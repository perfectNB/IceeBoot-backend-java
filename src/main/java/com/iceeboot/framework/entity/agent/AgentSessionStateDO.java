package com.iceeboot.framework.entity.agent;

import lombok.Data;

/** 数据库会话持久化记录。 */
@Data
public class AgentSessionStateDO {
    private Long revision;
    private byte[] payload;
}
