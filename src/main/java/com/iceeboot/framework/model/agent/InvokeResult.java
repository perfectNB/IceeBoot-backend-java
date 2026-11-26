package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.*;

import jakarta.validation.constraints.*;

/** 智能体运行时InvokeResult，仅供服务内部与发布快照使用。 */
public record InvokeResult(
        String runId, String sessionId, String output, long inputTokens, long outputTokens, Long cachedTokens, boolean usageKnown) {
    public InvokeResult(String runId,String output,long inputTokens,long outputTokens,boolean usageKnown) {
        this(runId,null,output,inputTokens,outputTokens,null,usageKnown);
    }
}
