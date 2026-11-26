package com.iceeboot.framework.vo.workflow;

import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class ApprovalViews {
    private ApprovalViews() {}
    public record Blueprint(int revision, int version, ApprovalDesign design) {}
    public record Role(String code, String name) {}
    public record Item(String id, String title, String status, int round, int revision, String node,
                       LocalDateTime updatedAt) {}
    public record Event(int round, String action, String node, long actorId, String actor, String reason,
                        LocalDateTime time, Map<String, String> values, String role) {}
    public record PendingTask(String id, String node, String role, boolean actionable) {}
    public record Detail(Item item, int version, ApprovalDesign design, Map<String, String> values,
                         List<Event> history, List<String> route, boolean mine, boolean canReview, List<PendingTask> tasks, Map<String,String> roleNames) {}
}
