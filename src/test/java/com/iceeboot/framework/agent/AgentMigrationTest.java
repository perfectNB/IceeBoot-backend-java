package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.service.database.AgentSchemaMigrationService;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.*;
import java.util.UUID;

class AgentMigrationTest {
    private DriverManagerDataSource source() {
        return new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
    }

    @Test
    void freshAndRepeatedMigrationCreateVersionedSchemaAndRejectModifiedHistory() {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        var migrations = new AgentSchemaMigrationService(source);
        migrations.migrateSchema();
        migrations.migrateSchema();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_agent_schema_history", Integer.class))
                .isEqualTo(6);
        jdbc.update("INSERT INTO t_agent_usage_quota(user_id,usage_day) VALUES('1',CURRENT_DATE)");
        jdbc.update("UPDATE t_agent_schema_history SET checksum='tampered' WHERE version=1");
        assertThatThrownBy(migrations::migrateSchema)
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_CHECKSUM_MISMATCH);
    }

    @Test
    void migrationAdoptsOldUnversionedTablesAndResumesPartiallyAppliedDdl() throws Exception {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        for (String sql :
                Files.readString(Path.of("src/main/resources/db/agent/V001__agent_baseline.sql"))
                        .split(";")) if (!sql.isBlank()) jdbc.execute(sql);
        jdbc.execute(
                "ALTER TABLE t_agent_run ADD COLUMN cancel_requested BOOLEAN NOT NULL DEFAULT"
                        + " FALSE");
        new AgentSchemaMigrationService(source).migrateSchema();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_agent_schema_history", Integer.class))
                .isEqualTo(6);
        jdbc.execute("SELECT cancel_requested,heartbeat_at,error_code,trace_id FROM t_agent_run");
    }

    @Test
    void nonemptyLegacyDatabaseCannotSilentlyStartWithEmptyNewTables() {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE ai_model_config(id BIGINT PRIMARY KEY)");
        jdbc.update("INSERT INTO ai_model_config VALUES(1)");
        assertThatThrownBy(() -> new AgentSchemaMigrationService(source).migrateSchema())
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_LEGACY_DATA_PENDING);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ai_model_config", Integer.class))
                .isEqualTo(1);
    }
}
