package com.iceeboot.framework.dto.update.workflow;

import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import java.util.Map;

public final class ApprovalRequests {
    private ApprovalRequests() {}
    public record Draft(int revision, ApprovalDesign design) {}
    public record Version(int revision) {}
    public record Submit(String code, int version, Map<String, String> values) {}
    public record Change(int revision, String reason, Map<String, String> values, String taskId) {
        public Change(int revision, String reason, Map<String,String> values) { this(revision,reason,values,null); }
    }
}
