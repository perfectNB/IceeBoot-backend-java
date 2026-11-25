package com.iceeboot.framework.entity.agent;

import lombok.Data;

@Data
public class MemoryJobDO {
    private long id;
    private String sourceRunId;
    private long sourceAgentId;
    private String sourceUserId;
    private String sourceSessionId;
    private String sourceReleaseId;
    private String policySnapshotCiphertext;
    private String requestCiphertext;
    private String executionRunId;
    private long leaseEpoch;
    private int attempts;
}
