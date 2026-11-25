package com.iceeboot.framework.entity.agent;

import lombok.Data;

@Data
public class MemoryGrantDO {
    private long revision;
    private String readFields;
    private String writeFields;
    private boolean canRead;
    private boolean canProposeUpdate;
}
