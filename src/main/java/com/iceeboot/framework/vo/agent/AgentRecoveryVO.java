package com.iceeboot.framework.vo.agent;
import java.util.List;
public record AgentRecoveryVO(AgentSessionVO session,String input,List<AgentFileVO> attachments,String sourceRunId) {}
