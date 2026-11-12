package com.iceeboot.framework.vo.agent;
import java.util.List;
public record AgentReleaseDiffVO(int fromVersion,int toVersion,List<Change> changes) {
    public record Change(String path,String before,String after,boolean sensitive) {}
}
