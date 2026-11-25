package com.iceeboot.framework.vo.agent;

import com.iceeboot.framework.model.agent.memory.MemoryData.FieldType;
import java.util.*;

public final class MemoryView {
    private MemoryView() {}

    public record Agent(long id, String name) {}
    public record Policy(long revision, boolean enabled, boolean reviewRequired, String prompt,
                         Map<String, FieldType> fields) {}
    public record Profile(long revision, Map<String, String> values, Map<String, FieldType> fields, Set<String> forgottenFields) {}
    public record Grant(long revision, Set<String> readFields, Set<String> writeFields, Set<String> availableFields) {}
    public record Candidate(long id, Map<String, String> values, Map<String, String> evidence) {}
}
