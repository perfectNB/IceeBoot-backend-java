package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.framework.controller.agent.*;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;

class AgentPermissionCatalogTest {
    @Test
    void migrationSeedsEveryDynamicEndpointAndMenuIdempotentlyForSuperAdminOnly() throws Exception {
        var source =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute(
                "CREATE TABLE t_menu(id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(120),name"
                    + " VARCHAR(120),path VARCHAR(200),component VARCHAR(120),icon"
                    + " VARCHAR(64),parent_id BIGINT,sort INT,type VARCHAR(24),hidden"
                    + " INT,keep_alive INT,requires_auth INT,status VARCHAR(24),permissions"
                    + " VARCHAR(200),create_time DATETIME,update_time DATETIME,del_flag INT DEFAULT"
                    + " 0)");
        jdbc.execute(
                "CREATE TABLE t_permission(id BIGINT AUTO_INCREMENT PRIMARY KEY,name"
                    + " VARCHAR(120),code VARCHAR(120),type VARCHAR(24),parent_id BIGINT,path"
                    + " VARCHAR(200),method VARCHAR(16),description VARCHAR(300),status"
                    + " VARCHAR(24),sort INT,create_time DATETIME,update_time DATETIME,del_flag INT"
                    + " DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE t_role(id BIGINT PRIMARY KEY,code VARCHAR(64),status"
                        + " VARCHAR(24),del_flag INT DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE t_role_menu(id BIGINT AUTO_INCREMENT PRIMARY KEY,role_id"
                    + " BIGINT,menu_id BIGINT,create_time DATETIME,update_time DATETIME,del_flag"
                    + " INT DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE t_role_permission(id BIGINT AUTO_INCREMENT PRIMARY KEY,role_id"
                        + " BIGINT,permission_id BIGINT,create_time DATETIME,update_time"
                        + " DATETIME,del_flag INT DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE t_user(id BIGINT PRIMARY KEY,username VARCHAR(64),status"
                        + " VARCHAR(24),del_flag INT DEFAULT 0)");
        jdbc.execute(
                "CREATE TABLE t_user_role(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,"
                        + "role_id BIGINT,create_time DATETIME,update_time DATETIME,"
                        + "del_flag INT DEFAULT 0)");
        jdbc.update(
                "INSERT INTO t_role(id,code,status) VALUES"
                        + " (1,'superAdmin','active'),(2,'admin','active')");
        String old =
                Files.readString(Path.of("docs/sql/20261110_agentscope_harness.sql"))
                        .substring(
                                Files.readString(
                                                Path.of("docs/sql/20261110_agentscope_harness.sql"))
                                        .indexOf("-- 新控制台替代"));
        try (Connection connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new org.springframework.core.io.ByteArrayResource(
                            old.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            // H2 使用 JDBC 开启事务；只去掉 MySQL 的事务命令，执行相同的菜单/权限 DML。
            String migration =
                    (Files.readString(Path.of("docs/sql/20261111_agent_console_permissions.sql"))
                                    + Files.readString(
                                            Path.of(
                                                    "docs/sql/20261111_agent_operations_permissions.sql")))
                            .replace("START TRANSACTION;", "")
                            .replace("COMMIT;", "");
            connection.setAutoCommit(false);
            for (int i = 0; i < 2; i++) {
                ScriptUtils.executeSqlScript(
                        connection,
                        new org.springframework.core.io.ByteArrayResource(
                                migration.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                connection.commit();
            }
        }
        var versioned =
                new com.iceeboot.framework.service.database.AgentSchemaMigrationService(source);
        versioned.migrateSchema();
        versioned.migratePermissions();
        versioned.migratePermissions();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_agent_schema_history", Integer.class))
                .isEqualTo(17);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_menu WHERE path LIKE"
                                        + " '/admin/agent-console%' AND del_flag=0",
                                Integer.class))
                .isEqualTo(10);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_permission WHERE path LIKE"
                                        + " '/v1/agent-console/%' AND del_flag=0",
                                Integer.class))
                .isEqualTo(85);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_role_permission WHERE role_id=1 AND"
                                        + " del_flag=0",
                                Integer.class))
                .isEqualTo(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM t_permission WHERE status='active' AND del_flag=0",
                        Integer.class));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_role_menu WHERE role_id=1 AND del_flag=0",
                                Integer.class))
                .isEqualTo(10);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_role_permission WHERE role_id=2",
                                Integer.class))
                .isZero();
        var keys = new HashSet<String>();
        for (Class<?> controller : AgentControllerCatalog.controllers()) {
            String root = controller.getAnnotation(RequestMapping.class).value()[0];
            for (var method : controller.getDeclaredMethods()) {
                String verb = null;
                String[] paths = null;
                if (method.isAnnotationPresent(GetMapping.class)) {
                    verb = "GET";
                    paths = method.getAnnotation(GetMapping.class).value();
                }
                if (method.isAnnotationPresent(PostMapping.class)) {
                    verb = "POST";
                    paths = method.getAnnotation(PostMapping.class).value();
                }
                if (method.isAnnotationPresent(PutMapping.class)) {
                    verb = "PUT";
                    paths = method.getAnnotation(PutMapping.class).value();
                }
                if (method.isAnnotationPresent(DeleteMapping.class)) {
                    verb = "DELETE";
                    paths = method.getAnnotation(DeleteMapping.class).value();
                }
                if (verb == null) continue;
                assertThat(method.getAnnotation(RequirePermission.class).dynamic())
                        .as(method.toString())
                        .isTrue();
                for (String path : paths.length == 0 ? new String[] {""} : paths) {
                    assertThat(keys.add(verb + " " + root + path)).isTrue();
                    assertThat(
                                    jdbc.queryForObject(
                                            "SELECT COUNT(*) FROM t_permission WHERE path=? AND"
                                                + " method=? AND status='active' AND del_flag=0",
                                            Integer.class,
                                            root + path,
                                            verb))
                            .as(verb + " " + root + path)
                            .isEqualTo(1);
                }
            }
        }
        assertThat(keys).hasSize(85);
    }
}
