package com.iceeboot.framework.service.workflow;

import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign.*;
import org.activiti.bpmn.model.*;
import org.activiti.bpmn.model.Process;
import java.util.*;

/** Compiles a published, immutable form design. Only generated identifiers enter engine expressions. */
final class ApprovalBpmn {
    private final Process process = new Process();
    private final Map<String,String> entries = new HashMap<>();
    private int flowNumber;

    static BpmnModel compile(ApprovalDesign design, String key) {
        return new ApprovalBpmn().build(design,key);
    }

    private BpmnModel build(ApprovalDesign design, String key) {
        var model = new BpmnModel();
        process.setId(key);
        process.setName(design.name());
        process.setExecutable(true);
        model.addProcess(process);
        add("start",new StartEvent());
        add("end",new EndEvent());
        entries.put(ApprovalRouting.END,"end");
        var exits = new ArrayList<String>();
        for (int i=0; i<design.nodes().size(); i++) {
            Node node = design.nodes().get(i);
            var roles = node.reviewers();
            String first = taskKey(i,0), last = first;
            if (roles.size() > 1) {
                first = add("fork_"+i,new ParallelGateway());
                last = add("join_"+i,new ParallelGateway());
            }
            entries.put(node.id(),first);
            exits.add(last);
            for (int slot=0; slot<roles.size(); slot++) {
                var task = new UserTask();
                task.setName("审批任务");
                task.setCandidateGroups(List.of(group(roles.get(slot))));
                String id = add(taskKey(i,slot),task);
                if (roles.size() > 1) {
                    connect(first,id);
                    connect(id,last);
                }
            }
        }
        route("start",design.start(),0);
        for (int i=0; i<exits.size(); i++) route(exits.get(i),design.nodes().get(i).exit(),i+1);
        return model;
    }

    static String taskKey(int node, int slot) { return "review_"+node+"_"+slot; }

    static String group(String role) {
        return "approval_"+UUID.nameUUIDFromBytes(role.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String add(String id, FlowElement element) {
        element.setId(id);
        process.addFlowElement(element);
        return id;
    }

    private SequenceFlow connect(String from,String to) {
        var flow = new SequenceFlow(from,to);
        add("flow_"+ ++flowNumber,flow);
        return flow;
    }

    private void route(String from, Exit exit, int index) {
        if (exit.branches().isEmpty()) {
            connect(from,entries.get(exit.fallback()));
            return;
        }
        var gateway = new ExclusiveGateway();
        String id = add("choice_"+index,gateway);
        connect(from,id);
        for (int i=0; i<exit.branches().size(); i++) {
            var branch = exit.branches().get(i);
            connect(id,entries.get(branch.target())).setConditionExpression("${"+flag(index,i)+"}");
        }
        // Missing fallback blocks submission in the form validator. Never silently approve it.
        String target = entries.get(exit.fallback());
        if (target != null) gateway.setDefaultFlow(connect(id,target).getId());
        else if (exit.branches().size() == 1) {
            // Activiti requires two outgoing flows for a conditional gateway.
            connect(id,"end").setConditionExpression("${false}");
        }
    }

    private static String flag(int exit, int branch) { return "rule_"+exit+"_"+branch; }

    static Map<String,Object> variables(ApprovalDesign design,Map<String,String> values) {
        // Validate the whole read-only form and ensure every reachable decision has a match.
        ApprovalRouting.plan(design,values);
        var fields = new HashMap<String,Field>();
        design.fields().forEach(f -> fields.put(f.key(),f));
        var exits = new ArrayList<Exit>();
        exits.add(design.start());
        design.nodes().forEach(n -> exits.add(n.exit()));
        var flags = new HashMap<String,Object>();
        for (int i=0; i<exits.size(); i++) {
            var branches = exits.get(i).branches();
            for (int j=0; j<branches.size(); j++) {
                boolean matched = ApprovalRouting.matches(branches.get(j),fields,values);
                flags.put(flag(i,j),matched);
            }
        }
        return flags;
    }
}
