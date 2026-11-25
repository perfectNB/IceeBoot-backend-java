package com.iceeboot.framework.entity.agent;

import lombok.Data;

@Data
public class MemoryProfileDO {
    private long id;
    private String scope;
    private long agentId;
    private String userId;
    private String profileCiphertext;
    private long revision;
    private long forgetEpoch;
    private String status;
}
