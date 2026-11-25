package com.iceeboot.framework.service.database;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.service.agent.support.AgentDiagnostics;

import lombok.RequiredArgsConstructor;

import org.springframework.boot.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.regex.*;

import javax.sql.DataSource;

/** Agent 数据库版本管理。MySQL DDL 隐式提交，按列/索引元数据恢复未完成升级。 */
@Service
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AgentSchemaMigrationService implements ApplicationRunner {
    private final DataSource source;
    private static final List<String> SCHEMA =
            List.of(
                    "V001__agent_baseline.sql",
                    "V002__agent_operations.sql",
                    "V003__active_code_indexes.sql", "V006__agent_sessions.sql", "V009__agent_progress.sql", "V012__agent_files.sql", "V014__agent_schedules.sql", "V016__agent_memory.sql");

    @Override
    public void run(ApplicationArguments args) {
        migrateSchema();
    }

    public void migrateSchema() {
        migrate(SCHEMA);
    }

    public void migratePermissions() {
        migrate(List.of("V004__operations_permissions.sql", "V005__framework_permissions.sql", "V007__session_permissions.sql", "V008__skill_import_permissions.sql", "V010__agent_stream_permissions.sql",
                "V011__authorization_indexes.sql", "V013__agent_workbench_permissions.sql", "V015__agent_schedule_permissions.sql", "V017__agent_memory_permissions.sql", "V018__embed_agent_memory_menu.sql", "V019__general_user_profiles.sql"));
    }

    private synchronized void migrate(List<String> migrations) {
        try (Connection connection = source.getConnection()) {
            boolean mysql =
                    connection
                            .getMetaData()
                            .getDatabaseProductName()
                            .toLowerCase(Locale.ROOT)
                            .contains("mysql");
            String lock = "iceeboot:agent:" + connection.getCatalog();
            boolean acquired = false;
            try {
                if (mysql) {
                    try (var s = connection.prepareStatement("SELECT GET_LOCK(?,30)")) {
                        s.setString(1, lock);
                        try (var rs = s.executeQuery()) {
                            acquired = rs.next() && rs.getInt(1) == 1;
                        }
                    }
                    if (!acquired)
                        throw new IceeBootException(ResponseMessage.Agent.SCHEMA_LOCK_FAILED);
                }
                try (var statement = connection.createStatement()) {
                    statement.execute(
                            "CREATE TABLE IF NOT EXISTS t_agent_schema_history(version INT NOT NULL"
                                + " PRIMARY KEY,description VARCHAR(160) NOT NULL,checksum"
                                + " VARCHAR(64) NOT NULL,installed_at TIMESTAMP NOT NULL DEFAULT"
                                + " CURRENT_TIMESTAMP)");
                }
                for (String migration : migrations) apply(connection, migration);
            } finally {
                if (acquired)
                    try (var s = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                        s.setString(1, lock);
                        s.execute();
                    }
            }
        } catch (IceeBootException e) {
            throw e;
        } catch (Exception e) {
            AgentDiagnostics.failure("schema", "MIGRATION", AgentDiagnostics.traceId(), e);
            throw new IceeBootException(ResponseMessage.Agent.SCHEMA_MIGRATION_FAILED, e);
        }
    }

    private void apply(Connection c, String name) throws Exception {
        int version = Integer.parseInt(name.substring(1, 4));
        String sql =
                new ClassPathResource("db/agent/" + name)
                        .getContentAsString(StandardCharsets.UTF_8);
        String checksum =
                HexFormat.of()
                        .formatHex(
                                MessageDigest.getInstance("SHA-256")
                                        .digest(sql.getBytes(StandardCharsets.UTF_8)));
        try (var statement =
                c.prepareStatement("SELECT checksum FROM t_agent_schema_history WHERE version=?")) {
            statement.setInt(1, version);
            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    if (!checksum.equals(rs.getString(1)))
                        throw new IceeBootException(ResponseMessage.Agent.SCHEMA_CHECKSUM_MISMATCH);
                    return;
                }
            }
        }
        if (version == 1) verifyLegacyData(c);
        // V011 依赖基础 RBAC 表，必须在 DatabaseInitService 完成后执行，DDL 不包在事务里。
        boolean transactional = !SCHEMA.contains(name) && version != 11;
        if (transactional) c.setAutoCommit(false);
        try {
            for (String statement : sql.replaceAll("(?m)^--[^\n]*", "").split(";")) {
                String ddl = statement.trim();
                if (ddl.isEmpty() || alreadyApplied(c, ddl)) continue;
                ScriptUtils.executeSqlScript(
                        c, new ByteArrayResource((ddl + ";").getBytes(StandardCharsets.UTF_8)));
            }
            try (var statement =
                    c.prepareStatement(
                            "INSERT INTO t_agent_schema_history(version,description,checksum)"
                                    + " VALUES(?,?,?)")) {
                statement.setInt(1, version);
                statement.setString(2, name);
                statement.setString(3, checksum);
                statement.executeUpdate();
            }
            if (transactional) c.commit();
        } catch (Exception e) {
            if (transactional) c.rollback();
            throw e;
        } finally {
            if (transactional) c.setAutoCommit(true);
        }
    }

    private void verifyLegacyData(Connection c) throws SQLException {
        String[][] pairs = {
            {"ai_model_config", "t_agent_model"},
            {"ai_capability", "t_agent_capability"},
            {"ai_agent_definition", "t_agent_definition"},
            {"ai_agent_release", "t_agent_release"},
            {"ai_agent_run", "t_agent_run"},
            {"ai_model_call", "t_agent_model_call"}
        };
        for (String[] pair : pairs) {
            if (!hasColumn(c, pair[0], "id")) continue;
            String query =
                    hasColumn(c, pair[1], "id")
                            ? "SELECT COUNT(*) FROM "
                                    + pair[0]
                                    + " s LEFT JOIN "
                                    + pair[1]
                                    + " t ON t.id=s.id WHERE t.id IS NULL"
                            : "SELECT COUNT(*) FROM " + pair[0];
            try (var statement = c.createStatement();
                    var rs = statement.executeQuery(query)) {
                if (rs.next() && rs.getLong(1) > 0)
                    throw new IceeBootException(ResponseMessage.Agent.SCHEMA_LEGACY_DATA_PENDING);
            }
        }
    }

    private boolean alreadyApplied(Connection c, String sql) throws SQLException {
        Matcher column =
                Pattern.compile("(?is)^ALTER TABLE (\\w+) ADD COLUMN (\\w+) .*").matcher(sql);
        if (column.matches()) return hasColumn(c, column.group(1), column.group(2));
        Matcher add =
                Pattern.compile("(?is)^ALTER TABLE (\\w+) ADD (?:UNIQUE )?INDEX (\\w+).*").matcher(sql);
        if (add.matches()) return hasIndex(c, add.group(1), add.group(2));
        Matcher drop = Pattern.compile("(?is)^ALTER TABLE (\\w+) DROP INDEX (\\w+).*").matcher(sql);
        return drop.matches() && !hasIndex(c, drop.group(1), drop.group(2));
    }

    private boolean hasColumn(Connection c, String table, String name) throws SQLException {
        for (String candidate : List.of(table, table.toUpperCase(Locale.ROOT)))
            try (var rs = c.getMetaData().getColumns(c.getCatalog(), null, candidate, null)) {
                while (rs.next())
                    if (name.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) return true;
            }
        return false;
    }

    private boolean hasIndex(Connection c, String table, String name) throws SQLException {
        for (String candidate : List.of(table, table.toUpperCase(Locale.ROOT)))
            try (var rs =
                    c.getMetaData().getIndexInfo(c.getCatalog(), null, candidate, false, false)) {
                while (rs.next())
                    if (name.equalsIgnoreCase(rs.getString("INDEX_NAME"))
                            || (c.getMetaData().getDatabaseProductName().equals("H2")
                                    && rs.getString("INDEX_NAME") != null
                                    && rs.getString("INDEX_NAME")
                                            .toUpperCase(Locale.ROOT)
                                            .startsWith(name.toUpperCase(Locale.ROOT) + "_INDEX_")))
                        return true;
            }
        return false;
    }
}
