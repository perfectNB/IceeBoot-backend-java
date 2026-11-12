package com.iceeboot.framework.vo.agent;
import com.iceeboot.framework.enums.agent.AgentFileKind;
import java.time.LocalDateTime;
public record AgentFileVO(String id,AgentFileKind kind,String name,long sizeBytes,String sha256,LocalDateTime createTime) {}
