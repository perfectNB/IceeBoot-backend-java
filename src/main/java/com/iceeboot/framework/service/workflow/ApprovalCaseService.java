package com.iceeboot.framework.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.utils.TokenUtils;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign.Node;
import com.iceeboot.framework.dto.update.workflow.ApprovalRequests.*;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.framework.vo.workflow.ApprovalViews.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ApprovalCaseService {
    private static final String PENDING = "PENDING", REJECTED = "REJECTED", APPROVED = "APPROVED", VOID = "VOID";
    private static final String COLUMNS = "id,title,status,round_no,revision,node_id,updated_at";
    private final JdbcTemplate jdbc;
    private final ApprovalDesignService designs;
    private final ISysUserService users;
    private final ObjectMapper json;
    private final ApprovalEngine engine;

    private record Application(Item item, long owner, String role, int version, ApprovalDesign design, Map<String,String> values) {}
    private long actor() {
        Long id = TokenUtils.getCurrentUserId();
        if (id == null) throw new IceeBootException("请先登录");
        return id;
    }

    public List<Item> list(String scope) {
        long user = actor();
        List<Object> args = new ArrayList<>();
        String filter;
        switch (scope) {
            case "inbox" -> {
                List<String> roles = users.getUserRoles(user);
                if (roles.isEmpty()) return List.of();
                String placeholders = String.join(",",Collections.nCopies(roles.size(),"?"));
                filter = "status='PENDING' AND (role_code IN ("+placeholders+") OR EXISTS ("+
                        "SELECT 1 FROM t_workflow_engine_round er JOIN ACT_RU_TASK t ON t.PROC_INST_ID_=er.instance_id "+
                        "JOIN ACT_RU_IDENTITYLINK l ON l.TASK_ID_=t.ID_ AND l.TYPE_='candidate' "+
                        "WHERE er.application_id=a.id AND er.round_no=a.round_no AND l.GROUP_ID_ IN ("+placeholders+")))";
                args.addAll(roles);
                roles.stream().map(ApprovalBpmn::group).forEach(args::add);
            }
            case "handled" -> {
                filter = "EXISTS (SELECT 1 FROM t_workflow_review_event e WHERE e.application_id=a.id AND e.actor_id=? AND e.action IN ('APPROVE','REJECT'))";
                args.add(user);
            }
            case "mine", "rejected" -> {
                filter = "owner_id=?" + ("rejected".equals(scope) ? " AND status='REJECTED'" : "");
                args.add(user);
            }
            default -> throw new IceeBootException("未知的审批列表");
        }
        return jdbc.query("SELECT " + COLUMNS + " FROM t_workflow_application a WHERE " + filter + " ORDER BY updated_at DESC,id",
                (r,i) -> item(r), args.toArray());
    }

    @Transactional(readOnly = true)
    public Detail detail(String id) {
        var a = load(id, false);
        long user = actor();
        boolean mine = a.owner == user;
        var roles = users.getUserRoles(user);
        String instance = engine.instance(id,a.item.round());
        var pending = engine.pending(instance,a.design,roles);
        boolean review = PENDING.equals(a.item.status()) && (instance == null ? roles.contains(a.role) :
                pending.stream().anyMatch(PendingTask::actionable));
        if (!mine && !review && jdbc.queryForObject("SELECT COUNT(*) FROM t_workflow_review_event WHERE application_id=? AND actor_id=? AND action IN ('APPROVE','REJECT')",
                Long.class, id, user) == 0L) throw new IceeBootException("无权查看该申请");
        var events = jdbc.query("SELECT * FROM t_workflow_review_event WHERE application_id=? ORDER BY id", (r,i) ->
                new Event(r.getInt("round_no"), r.getString("action"), r.getString("node_name"), r.getLong("actor_id"),
                        r.getString("actor_name"), r.getString("reason"), r.getTimestamp("created_at").toLocalDateTime(),
                        values(r.getString("submitted_form")),r.getString("review_role")), id);
        var visibleRoles = new HashSet<String>();
        a.design.nodes().forEach(n -> visibleRoles.addAll(n.reviewers()));
        var roleNames = new HashMap<String,String>();
        designs.roles().stream().filter(r -> visibleRoles.contains(r.code())).forEach(r -> roleNames.put(r.code(),r.name()));
        return new Detail(a.item, a.version, a.design, a.values, events,
                ApprovalRouting.plan(a.design,a.values).stream().map(Node::id).toList(), mine, review,pending,roleNames);
    }

    @Transactional
    public String submit(Submit request) {
        var b = designs.published(request.code(), request.version());
        String id = UUID.randomUUID().toString();
        String instance = engine.start(id,1,b.design(),b.version(),request.values());
        String first = engine.pending(instance,b.design(),List.of()).getFirst().node();
        String form = designs.write(request.values());
        long user = actor();
        jdbc.update("INSERT INTO t_workflow_application(id,blueprint_code,version,blueprint,title,owner_id,status,round_no,revision,node_id,role_code,form_data) VALUES(?,?,?,?,?,?,?,1,1,?,?,?)",
                id,b.design().code(),b.version(),designs.write(b.design()),b.design().name(),user,PENDING,
                first,"",form);
        event(id,1,"SUBMIT","发起申请",user,"",form);
        return id;
    }

    @Transactional
    public void review(String id, String action, Change request) {
        var a = load(id,true);
        String instance = engine.instance(id,a.item.round());
        if (a.item.revision() != request.revision() && (instance == null || request.taskId() == null))
            throw new IceeBootException("申请已更新，请刷新后处理");
        long user = actor();
        var roles = users.getUserRoles(user);
        if (!PENDING.equals(a.item.status()) || (instance == null && !roles.contains(a.role)))
            throw new IceeBootException("此任务已处理或不属于你的角色");
        if (request.values() != null) throw new IceeBootException("审批中不能修改申请内容");
        String reason = request.reason() == null ? "" : request.reason().strip();
        if (reason.length() > 2000 || ("reject".equals(action) && reason.isEmpty())) throw new IceeBootException("驳回时须填写理由，意见最多 2000 字");
        if (instance != null) {
            if (!"approve".equals(action) && !"reject".equals(action)) throw new IceeBootException("未知的审批操作");
            var task = ApprovalEngine.choose(engine.pending(instance,a.design,roles),request.taskId());
            boolean reject = "reject".equals(action);
            engine.decide(instance,task,reject,reason,user);
            var remaining = engine.pending(instance,a.design,roles);
            var status = reject ? REJECTED : remaining.isEmpty() ? APPROVED : PENDING;
            moveEngine(a,status,remaining,a.item.round(),a.values);
            var node = a.design.nodes().stream().filter(n -> n.id().equals(task.node())).findFirst().orElseThrow();
            event(id,a.item.round(),action.toUpperCase(Locale.ROOT),node.name(),user,reason,null,task.role());
            return;
        }
        // Only instances already in flight before the engine migration take this path.
        var path = ApprovalRouting.plan(a.design,a.values);
        int index = 0;
        while (index < path.size() && !path.get(index).id().equals(a.item.node())) index++;
        if (index == path.size()) throw new IllegalStateException("当前节点与已提交的路径不一致");
        Node next = null;
        String status;
        if ("approve".equals(action)) {
            if (index + 1 < path.size()) next = path.get(index + 1);
            status = next == null ? APPROVED : PENDING;
        } else if ("reject".equals(action)) status = REJECTED;
        else throw new IceeBootException("未知的审批操作");
        move(a,status,next,a.item.round(),a.values);
        event(id,a.item.round(),action.toUpperCase(Locale.ROOT),path.get(index).name(),user,reason,null);
    }

    @Transactional
    public void revise(String id, String action, Change request) {
        var a = current(id,request.revision());
        long user = actor();
        if (!REJECTED.equals(a.item.status()) || user != a.owner) throw new IceeBootException("只有发起人可以处理被驳回的申请");
        if ("void".equals(action)) {
            move(a,VOID,null,a.item.round(),a.values);
            event(id,a.item.round(),"VOID","作废申请",user,"",null);
        } else if ("resubmit".equals(action)) {
            int round = a.item.round()+1;
            String instance = engine.start(id,round,a.design,a.version,request.values());
            moveEngine(a,PENDING,engine.pending(instance,a.design,List.of()),round,request.values());
            event(id,round,"RESUBMIT","重新发起",user,"",designs.write(request.values()));
        } else throw new IceeBootException("未知的申请操作");
    }

    private void move(Application a,String status,Node next,int round,Map<String,String> values) {
        saveState(a,status,next == null ? "" : next.id(),next == null ? "" : next.role(),round,values);
    }

    private void moveEngine(Application a,String status,List<PendingTask> pending,int round,Map<String,String> values) {
        saveState(a,status,pending.isEmpty() ? "" : pending.getFirst().node(),"",round,values);
    }

    private void saveState(Application a,String status,String node,String role,int round,Map<String,String> values) {
        ApprovalDesignService.checkUpdated(jdbc.update("UPDATE t_workflow_application SET status=?,node_id=?,role_code=?,round_no=?,form_data=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND revision=?",
                status,node,role,round,designs.write(values),a.item.id(),a.item.revision()));
    }

    private Application current(String id,int revision) {
        var a = load(id,true);
        if (a.item.revision() != revision) throw new IceeBootException("申请已更新，请刷新后处理");
        return a;
    }
    private Application load(String id,boolean lock) {
        var found = jdbc.query("SELECT * FROM t_workflow_application WHERE id=?" + (lock ? " FOR UPDATE" : ""),
                (r,i) -> new Application(item(r),r.getLong("owner_id"),r.getString("role_code"),r.getInt("version"),
                        designs.read(r.getString("blueprint")), values(r.getString("form_data"))),id);
        if (found.isEmpty()) throw new IceeBootException("找不到申请");
        return found.get(0);
    }
    private Item item(ResultSet r) throws SQLException {
        return new Item(r.getString("id"),r.getString("title"),r.getString("status"),r.getInt("round_no"),
                r.getInt("revision"),r.getString("node_id"),r.getTimestamp("updated_at").toLocalDateTime());
    }
    private Map<String,String> values(String body) {
        if (body == null) return null;
        try { return json.readValue(body,new TypeReference<Map<String,String>>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("无法读取申请表单",e); }
    }
    private void event(String id,int round,String action,String node,long user,String reason,String form) {
        event(id,round,action,node,user,reason,form,"");
    }
    private void event(String id,int round,String action,String node,long user,String reason,String form,String role) {
        var account = users.getById(user);
        String name = account == null ? Long.toString(user) : account.getUsername();
        jdbc.update("INSERT INTO t_workflow_review_event(application_id,round_no,action,node_name,actor_id,actor_name,reason,submitted_form,review_role) VALUES(?,?,?,?,?,?,?,?,?)",
                id,round,action,node,user,name,reason,form,role);
    }
}
