package com.iceeboot.framework.dto.create.workflow;

import java.util.List;

/** Every exit has ordered conditional branches and an optional fallback. */
public record ApprovalDesign(String code, String name, List<Field> fields, Exit start, List<Node> nodes) {
    public record Field(String key, String label, String kind, String type, boolean required) {}
    public record Node(String id, String name, String role, Exit exit) {}
    public record Exit(List<Branch> branches, String fallback) {}
    public record Branch(List<Condition> conditions, String target) {}
    public record Condition(String field, String value) {}
}
