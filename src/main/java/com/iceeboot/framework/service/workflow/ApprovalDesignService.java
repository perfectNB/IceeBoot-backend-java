package com.iceeboot.framework.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.dto.update.workflow.ApprovalRequests.Draft;
import com.iceeboot.framework.vo.workflow.ApprovalViews.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalDesignService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ApprovalEngine engine;

    public List<Role> roles() {
        return jdbc.query("SELECT code,name FROM t_role WHERE status='active' AND del_flag=0 ORDER BY sort,id",
                (r,i) -> new Role(r.getString(1), r.getString(2)));
    }

    public List<Blueprint> designs(boolean published) {
        String sql = published ? "SELECT revision,version,published FROM t_workflow_blueprint WHERE version>0 ORDER BY code"
                : "SELECT revision,version,draft FROM t_workflow_blueprint ORDER BY code";
        return jdbc.query(sql, (r,i) -> new Blueprint(r.getInt(1), r.getInt(2), read(r.getString(3))));
    }

    @Transactional
    public void save(Draft request) {
        var d = request.design();
        ApprovalRouting.checkDraft(d);
        String body = write(d);
        if (body.length() > 250000) throw new IceeBootException("流程设计过大");
        if (request.revision() == 0) {
            try {
                jdbc.update("INSERT INTO t_workflow_blueprint(code,revision,version,draft) VALUES (?,1,0,?)", d.code(), body);
            } catch (DuplicateKeyException e) { throw new IceeBootException("流程码已存在"); }
        } else {
            int count = jdbc.update("UPDATE t_workflow_blueprint SET draft=?,revision=revision+1 WHERE code=? AND revision=?",
                    body, d.code(), request.revision());
            checkUpdated(count);
        }
    }

    @Transactional
    public void publish(String code, int revision) {
        var d = jdbc.query("SELECT draft FROM t_workflow_blueprint WHERE code=? AND revision=? FOR UPDATE",
                (r,i) -> read(r.getString(1)), code, revision);
        if (d.isEmpty()) throw new IceeBootException("草稿已更新，请刷新后再发布");
        ApprovalRouting.validate(d.get(0), roles().stream().map(Role::code).collect(Collectors.toSet()));
        int version = jdbc.queryForObject("SELECT version FROM t_workflow_blueprint WHERE code=?",Integer.class,code) + 1;
        engine.definition(d.getFirst(),version);
        jdbc.update("UPDATE t_workflow_blueprint SET published=draft,version=version+1,revision=revision+1 WHERE code=?", code);
    }

    Blueprint published(String code, int version) {
        var list = jdbc.query("SELECT revision,version,published FROM t_workflow_blueprint WHERE code=? AND version=? AND version>0",
                (r,i) -> new Blueprint(r.getInt(1), r.getInt(2), read(r.getString(3))), code, version);
        if (list.isEmpty()) throw new IceeBootException("流程版本已变化，请重新选择已发布流程");
        return list.get(0);
    }

    static void checkUpdated(int count) {
        if (count != 1) throw new IceeBootException("记录已更新，请刷新后重试");
    }
    ApprovalDesign read(String body) {
        try { return json.readValue(body, ApprovalDesign.class); }
        catch (JsonProcessingException e) { throw new IllegalStateException("无法读取流程设计", e); }
    }
    String write(Object data) {
        try { return json.writeValueAsString(data); }
        catch (JsonProcessingException e) { throw new IllegalStateException("无法保存流程数据", e); }
    }
}
