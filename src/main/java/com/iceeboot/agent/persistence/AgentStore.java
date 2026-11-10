package com.iceeboot.agent.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.agent.model.AgentContracts.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** 只管理本模块表；固定 SQL 与参数绑定，避免动态表名和字段拼接。 */
@Repository
public class AgentStore {
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;

  public AgentStore(JdbcTemplate jdbc, ObjectMapper json) {
    this.jdbc = jdbc;
    this.json = json;
  }

  public String json(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("配置无法序列化", e);
    }
  }

  public <T> T read(String value, Class<T> type) {
    try {
      return json.readValue(value, type);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("配置格式损坏", e);
    }
  }

  public List<ModelConfig> models() {
    return jdbc.query(
        "SELECT id,config_json FROM ai_model_config ORDER BY id DESC LIMIT 1000",
        (r, n) -> withId(read(r.getString(2), ModelConfig.class), r.getLong(1)));
  }

  public ModelConfig model(long id) {
    return jdbc
        .query(
            "SELECT id,config_json FROM ai_model_config WHERE id=?",
            (r, n) -> withId(read(r.getString(2), ModelConfig.class), r.getLong(1)),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("模型配置不存在"));
  }

  public long saveModel(ModelConfig m) {
    if (m.id() == null)
      return insert(
          "INSERT INTO ai_model_config(name,config_json) VALUES (?,?)", m.name(), json(m));
    if (jdbc.update(
            "UPDATE ai_model_config SET name=?,config_json=?,updated_at=CURRENT_TIMESTAMP WHERE"
                + " id=?",
            m.name(),
            json(m),
            m.id())
        != 1) throw new IllegalArgumentException("模型配置不存在");
    return m.id();
  }

  public void deleteModel(long id) {
    jdbc.update("DELETE FROM ai_model_config WHERE id=?", id);
  }

  public List<Capability> capabilities() {
    return jdbc.query(
        "SELECT id,config_json FROM ai_capability ORDER BY id DESC LIMIT 1000",
        (r, n) -> withId(read(r.getString(2), Capability.class), r.getLong(1)));
  }

  public Capability capability(long id) {
    return jdbc
        .query(
            "SELECT id,config_json FROM ai_capability WHERE id=?",
            (r, n) -> withId(read(r.getString(2), Capability.class), r.getLong(1)),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("能力不存在"));
  }

  public long saveCapability(Capability c) {
    if (c.id() == null)
      return insert(
          "INSERT INTO ai_capability(kind,code,config_json) VALUES (?,?,?)",
          c.kind().name(),
          c.code(),
          json(c));
    if (jdbc.update(
            "UPDATE ai_capability SET kind=?,code=?,config_json=?,updated_at=CURRENT_TIMESTAMP"
                + " WHERE id=?",
            c.kind().name(),
            c.code(),
            json(c),
            c.id())
        != 1) throw new IllegalArgumentException("能力不存在");
    return c.id();
  }

  public void deleteCapability(long id) {
    jdbc.update("DELETE FROM ai_capability WHERE id=?", id);
  }

  public List<AgentView> agents() {
    return jdbc.query(
        "SELECT a.*, COALESCE(p.version,0) active_version, (SELECT COUNT(*) FROM ai_agent_run r"
            + " WHERE r.agent_id=a.id) calls FROM ai_agent_definition a LEFT JOIN ai_agent_release"
            + " p ON p.id=a.active_release_id ORDER BY a.id DESC LIMIT 1000",
        (r, n) -> view(r));
  }

  public AgentView agent(long id) {
    return jdbc
        .query(
            "SELECT a.*, COALESCE(p.version,0) active_version, (SELECT COUNT(*) FROM ai_agent_run r"
                + " WHERE r.agent_id=a.id) calls FROM ai_agent_definition a LEFT JOIN"
                + " ai_agent_release p ON p.id=a.active_release_id WHERE a.id=?",
            (r, n) -> view(r),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("智能体不存在"));
  }

  public AgentView agentByCode(String code) {
    Long id =
        jdbc
            .query("SELECT id FROM ai_agent_definition WHERE code=?", (r, n) -> r.getLong(1), code)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("智能体不存在"));
    return agent(id);
  }

  public long saveAgent(AgentSpec a) {
    if (a.id() == null)
      return insert(
          "INSERT INTO ai_agent_definition(code,name,draft_json) VALUES (?,?,?)",
          a.code(),
          a.name(),
          json(a));
    if (jdbc.update(
            "UPDATE ai_agent_definition SET name=?,draft_json=?,updated_at=CURRENT_TIMESTAMP WHERE"
                + " id=? AND code=?",
            a.name(),
            json(a),
            a.id(),
            a.code())
        != 1) throw new IllegalArgumentException("智能体不存在或编码不可修改");
    return a.id();
  }

  public void deleteAgent(long id) {
    jdbc.update("DELETE FROM ai_agent_definition WHERE id=?", id);
  }

  public void lockAgent(long id) {
    jdbc.queryForObject("SELECT id FROM ai_agent_definition WHERE id=? FOR UPDATE", Long.class, id);
  }

  public int nextVersion(long id) {
    return jdbc.queryForObject(
        "SELECT COALESCE(MAX(version),0)+1 FROM ai_agent_release WHERE agent_id=?",
        Integer.class,
        id);
  }

  public void publish(PublishedAgent p, String user) {
    jdbc.update(
        "INSERT INTO ai_agent_release(id,agent_id,version,snapshot_json,created_by) VALUES"
            + " (?,?,?,?,?)",
        p.releaseId(),
        p.agent().id(),
        p.version(),
        json(p),
        user);
    jdbc.update(
        "UPDATE ai_agent_definition SET active_release_id=? WHERE id=?",
        p.releaseId(),
        p.agent().id());
  }

  public PublishedAgent release(String id) {
    return jdbc
        .query(
            "SELECT snapshot_json FROM ai_agent_release WHERE id=?",
            (r, n) -> read(r.getString(1), PublishedAgent.class),
            id)
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("智能体尚未发布"));
  }

  public List<ReleaseView> releases(long agent) {
    return jdbc.query(
        "SELECT id,version,created_at,created_by FROM ai_agent_release WHERE agent_id=? ORDER BY"
            + " version DESC LIMIT 100",
        (r, n) ->
            new ReleaseView(
                r.getString(1), r.getInt(2), r.getObject(3, LocalDateTime.class), r.getString(4)),
        agent);
  }

  public void startRun(
      String id, String root, String parent, PublishedAgent p, String user, String input) {
    jdbc.update(
        "INSERT INTO"
            + " ai_agent_run(id,root_run_id,parent_run_id,agent_id,agent_code,version,user_id,status,input_text,output_text)"
            + " VALUES (?,?,?,?,?,?,?,'RUNNING',?,'')",
        id,
        root,
        parent,
        p.agent().id(),
        p.agent().code(),
        p.version(),
        user,
        input);
  }

  public void finishRun(String id, String status, String output, String error) {
    jdbc.update(
        "UPDATE ai_agent_run SET status=?,output_text=?,error_text=?,finished_at=CURRENT_TIMESTAMP"
            + " WHERE id=?",
        status,
        output,
        error,
        id);
  }

  public void modelCall(
      String id,
      String run,
      String root,
      String agent,
      String model,
      String input,
      String output,
      long in,
      long out,
      boolean known) {
    jdbc.update(
        "INSERT INTO"
            + " ai_model_call(id,run_id,root_run_id,agent_code,model,input_text,output_text,input_tokens,output_tokens,usage_known)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?)",
        id,
        run,
        root,
        agent,
        model,
        input,
        output,
        in,
        out,
        known);
    jdbc.update(
        "UPDATE ai_agent_run SET input_tokens=input_tokens+?, output_tokens=output_tokens+?,"
            + " usage_known=usage_known AND ? WHERE id=?",
        in,
        out,
        known,
        run);
  }

  public List<RunView> runs() {
    return jdbc.query(
        "SELECT * FROM ai_agent_run WHERE id=root_run_id ORDER BY started_at DESC LIMIT 100",
        (r, n) -> run(r));
  }

  public RunDetail runDetail(String root) {
    List<RunView> agents =
        jdbc.query(
            "SELECT * FROM ai_agent_run WHERE root_run_id=? ORDER BY started_at,id",
            (r, n) -> run(r),
            root);
    if (agents.isEmpty()) throw new IllegalArgumentException("执行记录不存在");
    List<ModelCallView> calls =
        jdbc.query(
            "SELECT * FROM ai_model_call WHERE root_run_id=? ORDER BY created_at,id",
            (r, n) ->
                new ModelCallView(
                    r.getString("id"),
                    r.getString("run_id"),
                    r.getString("root_run_id"),
                    r.getString("agent_code"),
                    r.getString("model"),
                    r.getString("input_text"),
                    r.getString("output_text"),
                    r.getLong("input_tokens"),
                    r.getLong("output_tokens"),
                    r.getBoolean("usage_known"),
                    r.getObject("created_at", LocalDateTime.class)),
            root);
    return new RunDetail(
        agents,
        calls,
        calls.stream().mapToLong(ModelCallView::inputTokens).sum(),
        calls.stream().mapToLong(ModelCallView::outputTokens).sum(),
        calls.stream().allMatch(ModelCallView::usageKnown));
  }

  private long insert(String sql, Object... args) {
    GeneratedKeyHolder keys = new GeneratedKeyHolder();
    jdbc.update(
        c -> {
          var ps = c.prepareStatement(sql, new String[] {"id"});
          for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
          return ps;
        },
        keys);
    return keys.getKey().longValue();
  }

  private AgentView view(ResultSet r) throws SQLException {
    AgentSpec a = withId(read(r.getString("draft_json"), AgentSpec.class), r.getLong("id"));
    String active = r.getString("active_release_id");
    int version = r.getInt("active_version");
    return new AgentView(a, active, version, r.getLong("calls"));
  }

  private RunView run(ResultSet r) throws SQLException {
    return new RunView(
        r.getString("id"),
        r.getString("root_run_id"),
        r.getString("parent_run_id"),
        r.getLong("agent_id"),
        r.getString("agent_code"),
        r.getInt("version"),
        r.getString("user_id"),
        r.getString("status"),
        r.getString("input_text"),
        r.getString("output_text"),
        r.getString("error_text"),
        r.getLong("input_tokens"),
        r.getLong("output_tokens"),
        r.getBoolean("usage_known"),
        r.getObject("started_at", LocalDateTime.class),
        r.getObject("finished_at", LocalDateTime.class));
  }

  public static ModelConfig withId(ModelConfig m, Long id) {
    return new ModelConfig(
        id,
        m.name(),
        m.provider(),
        m.protocol(),
        m.baseUrl(),
        m.apiKey(),
        m.model(),
        m.contextWindow(),
        m.enabled(),
        m.generation());
  }

  public static Capability withId(Capability c, Long id) {
    return new Capability(
        id,
        c.kind(),
        c.code(),
        c.name(),
        c.description(),
        c.enabled(),
        c.skill(),
        c.tool(),
        c.mcp(),
        c.sandbox());
  }

  public static AgentSpec withId(AgentSpec a, Long id) {
    return new AgentSpec(
        id,
        a.code(),
        a.name(),
        a.description(),
        a.avatar(),
        a.prompt(),
        a.modelConfigId(),
        a.maxRounds(),
        a.enabled(),
        a.sandboxEnabled(),
        a.sandboxTemplateId(),
        a.planMode(),
        a.outputFormat(),
        a.outputSchema(),
        a.generation(),
        a.skillIds(),
        a.toolIds(),
        a.mcpIds(),
        a.knowledgeIds(),
        a.childAgentIds(),
        a.environment());
  }
}
