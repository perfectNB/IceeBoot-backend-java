package com.iceeboot.framework.vo.agent;
import java.util.List;
public record AgentPreflightVO(boolean passed, long durationMs, List<Check> checks) {
    public record Check(String name,String status,String detail,long durationMs) {}
}
