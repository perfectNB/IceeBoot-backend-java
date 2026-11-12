package com.iceeboot.framework.model.agent;

import com.iceeboot.framework.enums.agent.AgentProgressType;

/** 一轮执行内所有子智能体共享有序序号，正文增量不重复存入过程表。 */
public record AgentProgress(long sequence, AgentProgressType type, String rootRunId, String runId,
        String parentRunId, String agentCode, String title, String text, String callId,
        String toolName, String status, long timestamp, Long durationMs,
        Long inputTokens, Long outputTokens, Boolean usageKnown) {
    public AgentProgress(long sequence, AgentProgressType type, String rootRunId, String runId,
            String parentRunId, String agentCode, String title, String text, String callId,
            String toolName, String status, long timestamp) {
        this(sequence,type,rootRunId,runId,parentRunId,agentCode,title,text,callId,toolName,status,timestamp,null,null,null,null);
    }
}
