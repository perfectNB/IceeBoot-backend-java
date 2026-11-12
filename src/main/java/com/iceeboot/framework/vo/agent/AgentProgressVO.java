package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.enums.agent.AgentProgressType;
import lombok.Data;

/** 流式执行和历史过程使用相同响应契约。 */
@Data
public class AgentProgressVO {
    private long sequence;
    private AgentProgressType type;
    private String rootRunId;
    private String runId;
    private String parentRunId;
    private String agentCode;
    private String title;
    private String text;
    private String callId;
    private String toolName;
    private String status;
    private long timestamp;
    private Long durationMs;
    private Long inputTokens;
    private Long outputTokens;
    private Boolean usageKnown;
}
