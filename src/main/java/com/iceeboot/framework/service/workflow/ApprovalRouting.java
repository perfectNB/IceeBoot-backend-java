package com.iceeboot.framework.service.workflow;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign.*;
import java.math.BigDecimal;
import java.util.*;

public final class ApprovalRouting {
    public static final String END = "@end";
    private ApprovalRouting() {}

    public static void checkIdentity(ApprovalDesign d) {
        if (d == null || d.code() == null || !d.code().matches("[a-z][a-z0-9_]{0,63}"))
            fail("流程码须以小写字母开头，可包含数字和下划线，最长 64 位");
        if (!label(d.name())) fail("请填写流程名称，最长 120 字");
    }

    /** A draft can contain unfinished connections, but must remain editable. */
    public static void checkDraft(ApprovalDesign design) {
        checkIdentity(design);
        if (design.nodes() == null || design.fields() == null || design.nodes().size() > 100 || design.fields().size() > 100)
            fail("请检查节点或字段数量");
        for (Field f : design.fields()) {
            if (f == null || f.key() == null || f.label() == null || f.kind() == null || f.type() == null)
                fail("表单字段格式不完整");
        }
        checkDraftExit(design.start());
        for (Node n : design.nodes()) {
            if (n == null || !identifier(n.id()) || n.name() == null || n.role() == null)
                fail("审批节点格式不完整");
            checkDraftExit(n.exit());
        }
    }

    private static void checkDraftExit(Exit exit) {
        if (exit == null || exit.branches() == null || exit.branches().size() > 100)
            fail("流转配置格式不完整");
        for (Branch b : exit.branches()) {
            if (b == null || b.conditions() == null || b.conditions().size() > 100) fail("分支格式不完整");
            for (Condition c : b.conditions()) {
                if (c == null || c.field() == null || c.value() == null) fail("条件格式不完整");
            }
        }
    }

    public static void validate(ApprovalDesign d, Set<String> roles) {
        checkIdentity(d);
        if (d.fields() == null || d.fields().size() > 100 || d.nodes() == null || d.nodes().isEmpty()
                || d.nodes().size() > 100) fail("最多配置 100 个字段和 100 个审批节点，至少需要一个审批节点");
        Map<String, Field> fields = new HashMap<>();
        for (Field f : d.fields()) {
            if (f == null || !identifier(f.key()) || !label(f.label()) || fields.put(f.key(), f) != null)
                fail("请检查表单字段名称、标识及重复项");
            if (!"routing".equals(f.kind()) && !"business".equals(f.kind())) fail("请选择字段用途");
            if (!"text".equals(f.type()) && !"number".equals(f.type())) fail("请选择字段类型");
        }
        Map<String, Node> nodes = new HashMap<>();
        for (Node n : d.nodes()) {
            if (n == null || !identifier(n.id()) || !label(n.name()) || nodes.put(n.id(), n) != null)
                fail("审批节点缺少名称或标识重复");
            if (n.reviewers().size() > 50 || new HashSet<>(n.reviewers()).size() != n.reviewers().size() || !n.reviewers().stream().allMatch(r -> r != null && roles.contains(r))) fail("请选择有效的审批角色：" + n.name());
        }
        validateExit(d.start(), fields, nodes);
        if (END.equals(d.start().fallback()) || d.start().branches().stream().anyMatch(b -> END.equals(b.target())))
            fail("开始必须连接审批节点");
        for (Node n : nodes.values()) validateExit(n.exit(), fields, nodes);
        Set<String> visited = new HashSet<>();
        walk(d.start(), nodes, visited, new HashSet<>());
        if (visited.size() != nodes.size()) fail("有审批节点未连接到开始节点");
    }

    private static void validateExit(Exit exit, Map<String, Field> fields, Map<String, Node> nodes) {
        if (exit == null || exit.branches() == null || exit.branches().size() > 100) fail("请设置有效的流转连线");
        if (exit.fallback() != null && !exit.fallback().isEmpty()) target(exit.fallback(), nodes);
        else if (exit.branches().isEmpty()) fail("请选择下一节点或结束");
        for (Branch b : exit.branches()) {
            if (b == null || b.conditions() == null || b.conditions().isEmpty() || b.conditions().size() > 100)
                fail("条件分支必须至少包含一项判断");
            target(b.target(), nodes);
            Set<String> used = new HashSet<>();
            for (Condition c : b.conditions()) {
                Field f = c == null ? null : fields.get(c.field());
                if (f == null || (!"routing".equals(f.kind()) && !"number".equals(f.type())) || !used.add(c.field() + ":" + c.comparison())) fail("分支只能引用已定义且不重复的判断字段");
                if (!Set.of("eq","gt","ge","lt","le").contains(c.comparison()) ||
                        (!"eq".equals(c.comparison()) && !"number".equals(f.type()))) fail("请选择有效的比较方式");
                value(f, c.value(), true);
            }
        }
    }

    private static void walk(Exit exit, Map<String, Node> nodes, Set<String> visited, Set<String> visiting) {
        List<String> targets = new ArrayList<>();
        for (Branch b : exit.branches()) targets.add(b.target());
        targets.add(exit.fallback());
        for (String id : targets) {
            if (!nodes.containsKey(id)) continue;
            if (visiting.contains(id)) fail("审批路径存在循环，请调整连线");
            if (!visited.add(id)) continue;
            visiting.add(id);
            walk(nodes.get(id).exit(), nodes, visited, visiting);
            visiting.remove(id);
        }
    }

    public static List<Node> plan(ApprovalDesign design, Map<String,String> values) {
        if (values == null) fail("请填写申请表单");
        Map<String,Field> fields = new HashMap<>();
        for (Field f : design.fields()) {
            fields.put(f.key(), f);
            value(f, values.get(f.key()), f.required() || "routing".equals(f.kind()));
        }
        if (!fields.keySet().containsAll(values.keySet())) fail("表单包含未定义的字段");
        List<Node> plan = new ArrayList<>();
        Exit exit = design.start();
        while (true) {
            String next = exit.fallback();
            for (Branch b : exit.branches()) {
                if (matches(b,fields,values)) {
                    next = b.target();
                    break;
                }
            }
            if (END.equals(next)) return plan;
            String target = next;
            Node node = design.nodes().stream().filter(n -> n.id().equals(target)).findFirst()
                    .orElseThrow(() -> new IceeBootException("没有匹配的审批分支，请检查判断字段"));
            if (plan.contains(node)) fail("审批路径存在循环");
            plan.add(node);
            exit = node.exit();
        }
    }

    static boolean matches(Branch branch, Map<String,Field> fields, Map<String,String> values) {
        return branch.conditions().stream().allMatch(c -> matches(fields.get(c.field()),values.get(c.field()),c));
    }

    private static boolean matches(Field f, String actual, Condition condition) {
        if (actual == null || actual.isBlank()) return false;
        if (!"number".equals(f.type())) return actual.equals(condition.value());
        int cmp = new BigDecimal(actual).compareTo(new BigDecimal(condition.value()));
        return switch (condition.comparison()) {
            case "eq" -> cmp == 0;
            case "gt" -> cmp > 0;
            case "ge" -> cmp >= 0;
            case "lt" -> cmp < 0;
            case "le" -> cmp <= 0;
            default -> throw new IceeBootException("未知金额比较方式");
        };
    }

    private static void value(Field f, String raw, boolean required) {
        if (raw == null || raw.isBlank()) {
            if (required) fail("请填写：" + f.label());
            return;
        }
        if (raw.length() > 2000) fail("字段内容过长：" + f.label());
        if ("number".equals(f.type())) {
            try { new BigDecimal(raw); }
            catch (NumberFormatException e) { fail("请填写有效数字：" + f.label()); }
        }
    }

    private static boolean label(String s) { return s != null && !s.isBlank() && s.length() <= 120; }
    private static boolean identifier(String s) { return s != null && s.matches("[\\p{L}_][\\p{L}\\p{N}_]{0,63}"); }
    private static void target(String id, Map<String, Node> nodes) {
        if (!END.equals(id) && !nodes.containsKey(id)) fail("分支引用的节点不存在");
    }
    private static void fail(String message) { throw new IceeBootException(message); }
}
