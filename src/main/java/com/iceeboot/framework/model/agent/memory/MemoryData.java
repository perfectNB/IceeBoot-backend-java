package com.iceeboot.framework.model.agent.memory;

import com.iceeboot.framework.enums.agent.MemoryScope;
import java.util.*;

/** 字段名由画像策略定义。身份、版本和证据编号始终由服务端填写。 */
public final class MemoryData {
    private MemoryData() {}

    public enum FieldType { TEXT, NUMBER, BOOLEAN }

    public record Rule(MemoryScope scope, long agentId, long revision, boolean enabled,
                       boolean reviewRequired, String prompt, Map<String, FieldType> fields) {}

    public record Evidence(long messageId, String quote) {}
    public record Document(Map<String, String> values, Map<String, Evidence> evidence,
                           Set<String> forgotten) {
        public Document() {
            this(new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>());
        }
    }

    public record Target(Rule rule, long profileId, long revision, long forgetEpoch,
                         Set<String> writable, long grantRevision, long policyRevision) {}
    public record Turn(String context, List<Target> targets) {
        public static final Turn EMPTY = new Turn("", List.of());
    }

    public record Source(long messageId, String text) {}
    public record Suggestion(MemoryScope scope, String field, String value, String quote) {}
    public record Extraction(List<Suggestion> updates) {}
}
