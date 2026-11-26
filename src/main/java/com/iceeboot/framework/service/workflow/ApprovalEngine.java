package com.iceeboot.framework.service.workflow;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.vo.workflow.ApprovalViews.PendingTask;
import lombok.RequiredArgsConstructor;
import org.activiti.engine.*;
import org.activiti.engine.task.Task;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApprovalEngine {
    public static final String TENANT = "iceeboot-managed-approvals";
    private final JdbcTemplate jdbc;
    private final RepositoryService repository;
    private final RuntimeService runtime;
    private final TaskService tasks;

    /** Also deploys a pre-upgrade published snapshot on its first new submission. */
    String definition(ApprovalDesign design, int version) {
        var deployed = jdbc.queryForList("SELECT definition_id FROM t_workflow_engine_definition WHERE blueprint_code=? AND version=?",
                String.class,design.code(),version);
        if (!deployed.isEmpty()) return deployed.getFirst();
        // Only the first deployment locks the blueprint. Other cases can start independently.
        jdbc.queryForObject("SELECT code FROM t_workflow_blueprint WHERE code=? FOR UPDATE",String.class,design.code());
        var existing = jdbc.queryForList("SELECT definition_id FROM t_workflow_engine_definition WHERE blueprint_code=? AND version=? FOR UPDATE",
                String.class,design.code(),version);
        if (!existing.isEmpty()) return existing.getFirst();
        String key = "ia_"+UUID.nameUUIDFromBytes(design.code().getBytes(StandardCharsets.UTF_8)).toString().replace("-","");
        var deployment = repository.createDeployment().tenantId(TENANT).name(design.name())
                .addBpmnModel(key+".bpmn20.xml",ApprovalBpmn.compile(design,key)).deploy();
        var definition = repository.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        jdbc.update("INSERT INTO t_workflow_engine_definition(blueprint_code,version,definition_id) VALUES(?,?,?)",
                design.code(),version,definition.getId());
        return definition.getId();
    }

    String start(String application,int round,ApprovalDesign design,int version,Map<String,String> values) {
        var variables = ApprovalBpmn.variables(design,values);
        String id = runtime.startProcessInstanceById(definition(design,version),application,variables).getId();
        jdbc.update("INSERT INTO t_workflow_engine_round(application_id,round_no,instance_id) VALUES(?,?,?)",application,round,id);
        return id;
    }

    String instance(String application,int round) {
        return jdbc.queryForList("SELECT instance_id FROM t_workflow_engine_round WHERE application_id=? AND round_no=?",
                String.class,application,round).stream().findFirst().orElse(null);
    }

    List<PendingTask> pending(String instance,ApprovalDesign design,List<String> roles) {
        if (instance == null) return List.of();
        var active = new HashMap<String,Task>();
        tasks.createTaskQuery().processInstanceId(instance).list().forEach(t -> active.put(t.getTaskDefinitionKey(),t));
        var result = new ArrayList<PendingTask>();
        for (int i=0; i<design.nodes().size(); i++) {
            var node=design.nodes().get(i);
            var reviewers=node.reviewers();
            for (int j=0; j<reviewers.size(); j++) {
                Task task=active.remove(ApprovalBpmn.taskKey(i,j));
                String role=reviewers.get(j);
                if (task!=null) result.add(new PendingTask(task.getId(),node.id(),role,roles.contains(role)));
            }
        }
        if (!active.isEmpty()) throw new IllegalStateException("审批实例包含未知任务");
        return result;
    }

    void decide(String instance,PendingTask task,boolean reject,String reason,long actor) {
        if (reject) runtime.deleteProcessInstance(instance,reason);
        else {
            tasks.setAssignee(task.id(),Long.toString(actor));
            tasks.complete(task.id());
        }
    }

    static PendingTask choose(List<PendingTask> tasks,String id) {
        var allowed = tasks.stream().filter(PendingTask::actionable)
                .filter(t -> id == null || t.id().equals(id)).toList();
        if (allowed.size()!=1) throw new IceeBootException("请选择属于你的待办，任务已处理或角色不匹配");
        return allowed.getFirst();
    }
}
