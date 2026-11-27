package com.iceeboot.framework.dto.create.workflow;

import java.util.List;

/** Every exit has ordered conditional branches and an optional fallback. */
public record ApprovalDesign(String code, String name, List<Field> fields, Exit start, List<Node> nodes,
                             String launchRole, List<String> additionalLaunchRoles) {
    public ApprovalDesign(String code, String name, List<Field> fields, Exit start, List<Node> nodes, String launchRole) {
        this(code, name, fields, start, nodes, launchRole, List.of());
    }

    public ApprovalDesign(String code, String name, List<Field> fields, Exit start, List<Node> nodes) {
        this(code, name, fields, start, nodes, "");
    }
    public ApprovalDesign {
        launchRole = launchRole == null ? "" : launchRole.strip();
        additionalLaunchRoles = additionalLaunchRoles == null ? List.of() : List.copyOf(additionalLaunchRoles);
    }

    public boolean canLaunch(List<String> userRoles) {
        return launchRole.isEmpty() && additionalLaunchRoles.isEmpty()
                || userRoles.contains(launchRole) || !java.util.Collections.disjoint(userRoles, additionalLaunchRoles);
    }

    public record Field(String key, String label, String kind, String type, boolean required) {}
    public record Node(String id, String name, String role, Exit exit, List<String> coRoles) {
        public Node(String id, String name, String role, Exit exit) { this(id,name,role,exit,List.of()); }
        public List<String> reviewers() {
            var roles = new java.util.ArrayList<String>();
            roles.add(role);
            if (coRoles != null) roles.addAll(coRoles);
            return roles;
        }
    }
    public record Exit(List<Branch> branches, String fallback) {}
    public record Branch(List<Condition> conditions, String target) {}
    public record Condition(String field, String value, String operator) {
        public Condition(String field, String value) { this(field,value,"eq"); }
        public String comparison() { return operator == null ? "eq" : operator; }
    }
}
