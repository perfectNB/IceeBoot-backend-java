package com.iceeboot.framework.entity.agent;

import lombok.Data;

@Data
public class MemoryPolicyDO {
    private long id;
    private String scope;
    private long agentId;
    private String prompt;
    private String profileSchema;
    private long revision;
    private long modelConfigId;
    private boolean enabled;
    private boolean reviewRequired;
}
