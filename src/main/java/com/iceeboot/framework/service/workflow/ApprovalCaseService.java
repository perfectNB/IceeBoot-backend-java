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
                filter = "status='PENDING' AND role_code IN (" + String.join(",", Collections.nCopies(roles.size(), "?")) + ")";
                args.addAll(roles);
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
        boolean review = PENDING.equals(a.item.status()) && users.getUserRoles(user).contains(a.role);
        if (!mine && !review && jdbc.queryForObject("SELECT COUNT(*) FROM t_workflow_review_event WHERE application_id=? AND actor_id=? AND action IN ('APPROVE','REJECT')",
                Long.class, id, user) == 0L) throw new IceeBootException("无权查看该申请");
        var events = jdbc.query("SELECT * FROM t_workflow_review_event WHERE application_id=? ORDER BY id", (r,i) ->
                new Event(r.getInt("round_no"), r.getString("action"), r.getString("node_name"), r.getLong("actor_id"),
                        r.getString("actor_name"), r.getString("reason"), r.getTimestamp("created_at").toLocalDateTime(),
                        values(r.getString("submitted_form"))), id);
        return new Detail(a.item, a.version, a.design, a.values, events,
                ApprovalRouting.plan(a.design,a.values).stream().map(Node::id).toList(), mine, review);
    }

    @Transactional
    public String submit(Submit request) {
        var b = designs.published(request.code(), request.version());
        var plan = ApprovalRouting.plan(b.design(), request.values());
        Node first = plan.getFirst();
        String id = UUID.randomUUID().toString();
        String form = designs.write(request.values());
        long user = actor();
        jdbc.update("INSERT INTO t_workflow_application(id,blueprint_code,version,blueprint,title,owner_id,status,round_no,revision,node_id,role_code,form_data) VALUES(?,?,?,?,?,?,?,1,1,?,?,?)",
                id,b.design().code(),b.version(),designs.write(b.design()),b.design().name(),user,PENDING,
                first.id(),first.role(),form);
        event(id,1,"SUBMIT","发起申请",user,"",form);
        return id;
    }

    @Transactional
    public void review(String id, String action, Change request) {
        var a = current(id, request.revision());
        long user = actor();
        if (!PENDING.equals(a.item.status()) || !users.getUserRoles(user).contains(a.role))
            throw new IceeBootException("此任务已处理或不属于你的角色");
        if (request.values() != null) throw new IceeBootException("审批中不能修改申请内容");
        String reason = request.reason() == null ? "" : request.reason().strip();
        if (reason.length() > 2000 || ("reject".equals(action) && reason.isEmpty())) throw new IceeBootException("驳回时须填写理由，意见最多 2000 字");
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
            var path = ApprovalRouting.plan(a.design,request.values());
            Node first = path.getFirst();
            int round = a.item.round()+1;
            move(a,PENDING,first,round,request.values());
            event(id,round,"RESUBMIT","重新发起",user,"",designs.write(request.values()));
        } else throw new IceeBootException("未知的申请操作");
    }

    private void move(Application a,String status,Node next,int round,Map<String,String> values) {
        ApprovalDesignService.checkUpdated(jdbc.update("UPDATE t_workflow_application SET status=?,node_id=?,role_code=?,round_no=?,form_data=?,revision=revision+1,updated_at=CURRENT_TIMESTAMP WHERE id=? AND revision=?",
                status,next == null ? "" : next.id(),next == null ? "" : next.role(),round,designs.write(values),a.item.id(),a.item.revision()));
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
        var account = users.getById(user);
        String name = account == null ? Long.toString(user) : account.getUsername();
        jdbc.update("INSERT INTO t_workflow_review_event(application_id,round_no,action,node_name,actor_id,actor_name,reason,submitted_form) VALUES(?,?,?,?,?,?,?,?)",
                id,round,action,node,user,name,reason,form);
    }
}
