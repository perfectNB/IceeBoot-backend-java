package com.iceeboot.framework.service.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.core.domain.PageResult;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.dto.create.workflow.ApprovalDesign;
import com.iceeboot.framework.dto.update.workflow.ApprovalRequests.Draft;
import com.iceeboot.framework.vo.workflow.ApprovalViews.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import com.iceeboot.framework.service.system.ISysUserService;
import com.iceeboot.common.utils.TokenUtils;
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
    private final ISysUserService users;

    private static final String VISIBLE = """
             AND (launch_role='' OR launch_role IN (:roles) OR EXISTS (
                 SELECT 1 FROM t_workflow_launch_role lr
                 WHERE lr.blueprint_code=t_workflow_blueprint.code AND lr.version=t_workflow_blueprint.version
                   AND lr.role_code IN (:roles)))
            """;
    private MapSqlParameterSource audience() {
        Long user = TokenUtils.getCurrentUserId();
        List<String> roles = user == null ? List.of() : users.getUserRoles(user);
        return new MapSqlParameterSource("roles", roles.isEmpty() ? List.of("") : roles);
    }
    private NamedParameterJdbcTemplate catalog() {
        return new NamedParameterJdbcTemplate(jdbc);
    }

    public List<Role> roles() {
        return jdbc.query("SELECT code,name FROM t_role WHERE status='active' AND del_flag=0 ORDER BY sort,id",
                (r,i) -> new Role(r.getString(1), r.getString(2)));
    }

    public List<Blueprint> designs(boolean published) {
        String sql = published ? "SELECT revision,version,published,stopped FROM t_workflow_blueprint WHERE version>0 AND stopped=FALSE" + VISIBLE + " ORDER BY code"
                : "SELECT revision,version,draft,stopped FROM t_workflow_blueprint ORDER BY code";
        return catalog().query(sql, published ? audience() : new MapSqlParameterSource(),
                (r,i) -> new Blueprint(r.getInt(1), r.getInt(2), read(r.getString(3)), r.getBoolean("stopped")));
    }

    @Transactional(readOnly = true)
    public PageResult<TemplateRow> page(boolean publishedOnly, int page, int size) {
        return page(publishedOnly, page, size, null);
    }

    @Transactional(readOnly = true)
    public PageResult<TemplateRow> page(boolean publishedOnly, int page, int size, String keyword) {
        return page(publishedOnly, page, size, keyword, null);
    }

    @Transactional(readOnly = true)
    public PageResult<TemplateRow> page(boolean publishedOnly, int page, int size, String keyword, String status) {
        if (page < 1 || size < 1 || size > 100) throw new IceeBootException("分页参数无效");
        String from = " FROM t_workflow_blueprint WHERE " + (publishedOnly ? "version>0" + VISIBLE : "1=1");
        var params = publishedOnly ? audience() : new MapSqlParameterSource();
        // The launch picker requests published templates with a keyword (including the empty string).
        String filter = status == null ? (publishedOnly && keyword != null ? "published" : "all") : status;
        from += switch (filter) {
            case "all" -> "";
            case "draft" -> " AND version=0";
            case "published" -> " AND version>0 AND stopped=FALSE";
            case "stopped" -> " AND stopped=TRUE";
            default -> throw new IceeBootException("未知的流程状态");
        };
        if (keyword != null && !keyword.isBlank()) {
            from += " AND LOCATE(LOWER(:keyword), LOWER(CASE WHEN version=0 THEN draft_name ELSE published_name END))>0";
            params.addValue("keyword", keyword.strip());
        }
        var query = catalog();
        long total = query.queryForObject("SELECT COUNT(*)" + from, params, Long.class);
        long current = Math.min(page, Math.max(1L, (total + size - 1) / size));
        String body = publishedOnly ? "published" : "draft";
        String publishedBody = publishedOnly ? "NULL" : "published";
        var rows = query.query("SELECT code,revision,version,stopped," + body + " AS body," + publishedBody
                        + " AS live_body" + from + " ORDER BY code LIMIT :size OFFSET :offset",
                params.addValue("size",size).addValue("offset",(current - 1) * size),
                (r, i) -> {
                    String snapshot = r.getString("body");
                    String released = r.getString("live_body");
                    return new TemplateRow(r.getString("code"), read(released == null ? snapshot : released).name(), r.getInt("revision"),
                            r.getInt("version"), r.getBoolean("stopped"), released != null && !released.equals(snapshot));
                });
        return PageResult.of(rows, total, current, (long) size);
    }

    public Blueprint detail(String code, boolean publishedOnly) {
        return detail(code, publishedOnly, publishedOnly);
    }

    public Blueprint managementDetail(String code, boolean published) {
        return detail(code, published, false);
    }

    private Blueprint detail(String code, boolean publishedOnly, boolean checkAudience) {
        String body = publishedOnly ? "published" : "draft";
        String sql = "SELECT revision,version," + body + ",stopped FROM t_workflow_blueprint WHERE code=:code"
                + (publishedOnly ? " AND version>0" : "") + (checkAudience ? VISIBLE : "");
        var params = checkAudience ? audience() : new MapSqlParameterSource();
        return catalog().query(sql, params.addValue("code", code), (r,i) -> new Blueprint(r.getInt(1), r.getInt(2), read(r.getString(3)), r.getBoolean("stopped")))
                .stream().findFirst().orElseThrow(() -> new IceeBootException("流程模板不存在，请刷新"));
    }

    @Transactional
    public void setStopped(String code, int revision, boolean stopped) {
        checkUpdated(jdbc.update("UPDATE t_workflow_blueprint SET stopped=?,revision=revision+1 "
                + "WHERE code=? AND revision=? AND version>0", stopped, code, revision));
    }

    @Transactional
    public void save(Draft request) {
        var d = request.design();
        ApprovalRouting.checkDraft(d);
        String body = write(d);
        if (body.length() > 250000) throw new IceeBootException("流程设计过大");
        if (request.revision() == 0) {
            try {
                jdbc.update("INSERT INTO t_workflow_blueprint(code,revision,version,draft,draft_name) VALUES (?,1,0,?,?)", d.code(), body, d.name());
            } catch (DuplicateKeyException e) { throw new IceeBootException("流程码已存在"); }
        } else {
            int count = jdbc.update("UPDATE t_workflow_blueprint SET draft=?,draft_name=?,revision=revision+1 WHERE code=? AND revision=?",
                    body, d.name(), d.code(), request.revision());
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
        var design = d.getFirst();
        engine.definition(design,version);
        jdbc.update("DELETE FROM t_workflow_launch_role WHERE blueprint_code=?", code);
        var additionalRoles = design.additionalLaunchRoles().stream().distinct().toList();
        if (!additionalRoles.isEmpty()) {
            jdbc.batchUpdate("INSERT INTO t_workflow_launch_role(blueprint_code,version,role_code) VALUES(?,?,?)",
                    additionalRoles, 100, (statement, role) -> {
                        statement.setString(1, code);
                        statement.setInt(2, version);
                        statement.setString(3, role);
                    });
        }
        jdbc.update("UPDATE t_workflow_blueprint SET published=draft,published_name=?,launch_role=?,version=version+1,revision=revision+1 WHERE code=?",
                d.getFirst().name(), d.getFirst().launchRole(), code);
    }

    Blueprint published(String code, int version) {
        var list = jdbc.query("SELECT revision,version,published,stopped FROM t_workflow_blueprint WHERE code=? AND version=? AND version>0 FOR UPDATE",
                (r,i) -> {
                    if (r.getBoolean("stopped")) throw new IceeBootException("流程已停用，无法发起新申请");
                    return new Blueprint(r.getInt(1), r.getInt(2), read(r.getString(3)), r.getBoolean("stopped"));
                }, code, version);
        if (list.isEmpty()) throw new IceeBootException("流程版本已变化，请重新选择已发布流程");
        var blueprint = list.getFirst();
        if (!blueprint.design().canLaunch(users.getUserRoles(TokenUtils.getCurrentUserId())))
            throw new IceeBootException("无权发起该流程");
        return blueprint;
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
