package com.iceeboot.framework.entity.agent;

import lombok.Data;

@Data
public class MemoryChangeDO {
    private long id;
    private long profileId;
    private long jobId;
    private long baseRevision;
    private long baseForgetEpoch;
    private String status;
    private String patchCiphertext;
    private String evidenceCiphertext;
}
