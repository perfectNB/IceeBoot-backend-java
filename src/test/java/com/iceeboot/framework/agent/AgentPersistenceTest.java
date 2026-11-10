package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/** 使用真实 MyBatis Mapper 和事务代理验证分表持久化。 */
class AgentPersistenceTest {
    @Test
    void typedResourcesRoundTripWithSharedIdsAuditAndLogicalDeletion() throws Exception {
        var f = new AgentFixture();
        var tool = f.http("lookup", "https://example.invalid/search");
        var skill =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.SKILL,
                                "analyze",
                                "分析",
                                "说明",
                                true,
                                new SkillConfig(
                                        "读取资源并分析",
                                        Map.of("data/sample.txt", "sample"),
                                        List.of(tool.id())),
                                null,
                                null,
                                null));
        var mcp =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.MCP,
                                "remote",
                                "远程 MCP",
                                "说明",
                                true,
                                null,
                                null,
                                new McpConfig(
                                        "https://example.invalid/mcp",
                                        Map.of("Authorization", "secret-header"),
                                        List.of("search")),
                                null));
        var sandbox =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.SANDBOX,
                                "python",
                                "Python 沙箱",
                                "说明",
                                true,
                                null,
                                null,
                                null,
                                new SandboxConfig("python-template", 300)));
        var knowledge =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.KNOWLEDGE,
                                "documents",
                                "知识库",
                                "说明",
                                true,
                                null,
                                new ToolConfig(
                                        ToolKind.HTTP,
                                        "https://example.invalid/retrieve",
                                        "POST",
                                        Map.of(),
                                        Map.of("type", "object"),
                                        null,
                                        false,
                                        null),
                                null,
                                null));
        var resources =
                Map.of(
                        "t_agent_tool",
                        tool,
                        "t_agent_skill",
                        skill,
                        "t_agent_mcp",
                        mcp,
                        "t_agent_sandbox_template",
                        sandbox,
                        "t_agent_knowledge",
                        knowledge);
        assertThat(f.store.capabilities())
                .extracting(Capability::id)
                .containsExactlyInAnyOrderElementsOf(
                        resources.values().stream().map(Capability::id).toList());
        for (var entry : resources.entrySet()) {
            var row =
                    f.jdbc.queryForMap(
                            "SELECT id,code,create_time,del_flag FROM "
                                    + entry.getKey()
                                    + " WHERE id=?",
                            entry.getValue().id());
            assertThat(((Number) row.get("id")).longValue()).isEqualTo(entry.getValue().id());
            assertThat(row.get("code")).isEqualTo(entry.getValue().code());
            assertThat(row.get("create_time")).isNotNull();
            assertThat(f.store.capability(entry.getValue().id()).kind())
                    .isEqualTo(entry.getValue().kind());
        }
        assertThat(f.store.capability(skill.id()).skill().resources())
                .containsEntry("data/sample.txt", "sample");
        assertThat(f.store.capability(skill.id()).skill().toolIds()).containsExactly(tool.id());
        String storedHeaders =
                f.jdbc.queryForObject(
                        "SELECT headers_json FROM t_agent_mcp WHERE id=?", String.class, mcp.id());
        assertThat(storedHeaders).contains("enc:v2:primary:").doesNotContain("secret-header");
        assertThat(f.configs.capability(mcp.id()).mcp().headers())
                .containsEntry("Authorization", "********");
        for (var entry : resources.entrySet()) {
            f.store.deleteCapability(entry.getValue().id());
            assertThat(
                            f.jdbc.queryForObject(
                                    "SELECT del_flag FROM " + entry.getKey() + " WHERE id=?",
                                    Integer.class,
                                    entry.getValue().id()))
                    .isEqualTo(1);
            assertThat(
                            f.jdbc.queryForObject(
                                    "SELECT del_flag FROM t_agent_capability WHERE id=?",
                                    Integer.class,
                                    entry.getValue().id()))
                    .isEqualTo(1);
            assertThatThrownBy(() -> f.store.capability(entry.getValue().id()))
                    .hasMessageContaining("不存在");
        }
        assertThat(f.store.capabilities()).isEmpty();
    }

    @Test
    void failedDetailInsertRollsBackRegistryAllocation() throws Exception {
        var f = new AgentFixture();
        f.jdbc.execute(
                "ALTER TABLE t_agent_skill ADD CONSTRAINT test_content CHECK (LENGTH(content) <"
                        + " 10)");
        assertThatThrownBy(
                        () ->
                                f.configs.saveCapability(
                                        new Capability(
                                                null,
                                                CapabilityKind.SKILL,
                                                "too_long",
                                                "测试",
                                                "",
                                                true,
                                                new SkillConfig(
                                                        "this content exceeds the test database"
                                                                + " constraint",
                                                        Map.of(),
                                                        List.of()),
                                                null,
                                                null,
                                                null)))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_capability", Integer.class))
                .isZero();
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_skill", Integer.class))
                .isZero();
    }

    @Test
    void failedUsageUpdateRollsBackModelCall() throws Exception {
        var f = new AgentFixture();
        assertThatThrownBy(
                        () ->
                                f.store.modelCall(
                                        "call",
                                        "missing-run",
                                        "root",
                                        "agent",
                                        "model",
                                        "input",
                                        "output",
                                        1,
                                        2,
                                        true))
                .hasMessageContaining("执行记录不存在");
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_model_call", Integer.class))
                .isZero();
    }

    @Test
    void fullToolUpdateClearsObsoleteHttpFields() throws Exception {
        var f = new AgentFixture();
        var tool = f.http("lookup", "https://example.invalid/search");
        f.configs.saveCapability(
                new Capability(
                        tool.id(),
                        CapabilityKind.TOOL,
                        tool.code(),
                        tool.name(),
                        "",
                        true,
                        null,
                        new ToolConfig(
                                ToolKind.SCRIPT,
                                null,
                                null,
                                Map.of(),
                                Map.of("type", "object"),
                                "python script.py",
                                false,
                                null),
                        null,
                        null));
        var saved = f.store.capability(tool.id()).tool();
        assertThat(saved.kind()).isEqualTo(ToolKind.SCRIPT);
        assertThat(saved.url()).isNull();
        assertThat(saved.method()).isNull();
        assertThat(saved.script()).isEqualTo("python script.py");
    }
}
