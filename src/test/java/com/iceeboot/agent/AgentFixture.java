package com.iceeboot.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.agent.config.AgentProperties;
import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.persistence.AgentStore;
import com.iceeboot.agent.security.AgentSecrets;
import com.iceeboot.agent.service.AgentConfigurationService;
import java.nio.file.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AgentFixture {
  final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  final AgentProperties properties = new AgentProperties();
  final JdbcTemplate jdbc;
  final AgentStore store;
  final AgentSecrets secrets;
  final AgentConfigurationService configs;

  AgentFixture() throws Exception {
    var data =
        new DriverManagerDataSource(
            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    jdbc = new JdbcTemplate(data);
    String ddl =
        Files.readString(Path.of("docs/sql/20261110_agentscope_harness.sql")).split("-- 新控制台替代")[0];
    for (String statement : ddl.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
    properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
    properties.setWorkspace(Files.createTempDirectory("iceeboot-agent-test-").toString());
    store = new AgentStore(jdbc, json);
    secrets = new AgentSecrets(properties);
    configs = new AgentConfigurationService(store, secrets);
  }

  ModelConfig model() {
    return configs.saveModel(
        new ModelConfig(
            null,
            "测试模型",
            Provider.GPT,
            Protocol.OPENAI_CHAT,
            "https://example.invalid/v1",
            "test-key",
            "test-model",
            16384,
            true,
            null));
  }

  AgentSpec draft(
      String code,
      long model,
      List<Long> skills,
      List<Long> tools,
      List<Long> mcps,
      List<Long> children,
      boolean sandbox,
      Long template) {
    return new AgentSpec(
        null,
        code,
        code,
        "测试职责",
        "",
        "帮助用户完成任务",
        model,
        6,
        true,
        sandbox,
        template,
        false,
        OutputFormat.TEXT,
        Map.of(),
        null,
        skills,
        tools,
        mcps,
        List.of(),
        children,
        Map.of());
  }

  AgentView save(
      String code,
      long model,
      List<Long> skills,
      List<Long> tools,
      List<Long> mcps,
      List<Long> children,
      boolean sandbox,
      Long template) {
    return configs.saveAgent(draft(code, model, skills, tools, mcps, children, sandbox, template));
  }

  PublishedAgent publish(AgentView a) {
    return store.release(configs.publish(a.draft().id(), "1").id());
  }

  Capability http(String code, String url) {
    return configs.saveCapability(
        new Capability(
            null,
            CapabilityKind.TOOL,
            code,
            code,
            "查询数据",
            true,
            null,
            new ToolConfig(
                ToolKind.HTTP,
                url,
                "GET",
                Map.of(),
                Map.of("type", "object", "properties", Map.of()),
                null,
                false,
                null),
            null,
            null));
  }
}
