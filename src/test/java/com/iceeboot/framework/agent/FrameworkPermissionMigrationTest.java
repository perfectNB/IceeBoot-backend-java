package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.annotation.RequirePermission;
import com.iceeboot.framework.service.database.AgentSchemaMigrationService;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.*;

/** 从历史 RBAC 状态升级，验证管理员完整授权与普通角色授权边界。 */
class FrameworkPermissionMigrationTest {
    private static final String CURRENT_MODEL_LIST = "/v1/agent-console/models";
    private static final String CURRENT_AGENT_LIST = "/v1/agent-console/agents";
    private static final String LEGACY_AGENT_LIST = "/v1/agents";

    @Test
    void upgradesHistoricalPermissionsIdempotentlyAndSeedsEveryDeclaredEndpoint() throws Exception {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        seedHistoricalState(source, jdbc);
        var migrations = new AgentSchemaMigrationService(source);
        migrations.migrateSchema();
        migrations.migratePermissions();
        List<Map<String, Object>> before = rbacSnapshot(jdbc);
        migrations.migratePermissions();
        assertSameSnapshot(before, rbacSnapshot(jdbc));
        assertThat(count(jdbc, "SELECT COUNT(*) FROM t_agent_schema_history")).isEqualTo(5);

        List<Endpoint> expected = catalog();
        assertThat(expected).hasSize(219);
        assertThat(expected.stream().map(Endpoint::key)).doesNotHaveDuplicates();
        assertThat(expected.stream().map(Endpoint::code)).doesNotHaveDuplicates();
        assertThat(dynamicControllerEndpoints()).containsExactlyInAnyOrderElementsOf(
                expected.stream().map(Endpoint::key).toList());
        for (Endpoint endpoint : expected) {
            var actual = jdbc.queryForList(
                    "SELECT p.name,p.code,parent.code parent_code FROM t_permission p"
                            + " JOIN t_permission parent ON parent.id=p.parent_id"
                            + " WHERE p.type='api' AND p.status='active' AND p.del_flag=0"
                            + " AND p.path=? AND p.method=? AND parent.del_flag=0",
                    endpoint.path(), endpoint.method());
            assertThat(actual).as(endpoint.key()).singleElement().satisfies(permission -> {
                assertThat(permission.get("NAME")).isEqualTo(endpoint.name());
                assertThat(permission.get("CODE")).isEqualTo(endpoint.code());
                assertThat(permission.get("PARENT_CODE")).isEqualTo(endpoint.parentCode());
            });
            assertThat(endpoint.code()).endsWith(":api");
            assertThat(endpoint.name()).containsPattern("[\\u4e00-\\u9fff]")
                    .doesNotMatch("(?i)^(GET|POST|PUT|DELETE|PATCH) .*?");
        }
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM (SELECT code FROM t_permission WHERE del_flag=0"
                        + " GROUP BY code HAVING COUNT(*)>1) duplicate_codes")).isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission WHERE type='api' AND del_flag=0"
                        + " AND code NOT LIKE '%:api'")).isZero();
    }

    @Test
    void repeatingUpgradeSqlPreservesPermissionRowsAndRoleGrants() throws Exception {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        seedHistoricalState(source, jdbc);
        var migrations = new AgentSchemaMigrationService(source);
        migrations.migrateSchema();
        migrations.migratePermissions();
        List<Map<String, Object>> before = rbacSnapshot(jdbc);
        try (var connection = source.getConnection()) {
            for (int repeat = 0; repeat < 2; repeat++) {
                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/agent/V005__framework_permissions.sql"));
            }
        }
        assertSameSnapshot(before, rbacSnapshot(jdbc));
    }

    @Test
    void superAdminReceivesAllActivePermissionsAndMenusWithoutUpgradingOrdinaryRole() throws Exception {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        seedHistoricalState(source, jdbc);
        var migrations = new AgentSchemaMigrationService(source);
        migrations.migrateSchema();
        migrations.migratePermissions();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission p WHERE p.status='active' AND p.del_flag=0"
                        + " AND NOT EXISTS(SELECT 1 FROM t_role_permission rp"
                        + " WHERE rp.role_id=1 AND rp.permission_id=p.id AND rp.del_flag=0)"))
                .isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_menu m WHERE m.status='active' AND m.del_flag=0"
                        + " AND NOT EXISTS(SELECT 1 FROM t_role_menu rm"
                        + " WHERE rm.role_id=1 AND rm.menu_id=m.id AND rm.del_flag=0)"))
                .isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_user_role WHERE user_id=101 AND role_id=1 AND del_flag=0"))
                .isEqualTo(1);
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_user_role WHERE user_id=102 AND role_id=1 AND del_flag=0"))
                .isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_role_menu WHERE role_id=2 AND del_flag=0"))
                .isEqualTo(1);
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_role_permission rp JOIN t_permission p"
                        + " ON p.id=rp.permission_id WHERE rp.role_id=2 AND rp.del_flag=0"
                        + " AND p.type='api' AND p.del_flag=0"))
                .isEqualTo(1);
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_role_permission WHERE role_id=2 AND permission_id=900"
                        + " AND del_flag=0")).isEqualTo(1);
    }

    @Test
    void preservesHarnessRouteGrantsWithoutImportingCollidingLegacyAgentGrants() throws Exception {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        seedHistoricalState(source, jdbc);
        var migrations = new AgentSchemaMigrationService(source);
        migrations.migrateSchema();
        migrations.migratePermissions();
        var model = catalog().stream().filter(endpoint -> endpoint.method().equals("GET")
                && endpoint.path().equals(CURRENT_MODEL_LIST)).findFirst().orElseThrow();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_role_permission rp JOIN t_permission p"
                        + " ON p.id=rp.permission_id WHERE rp.role_id=2 AND rp.del_flag=0"
                        + " AND p.del_flag=0 AND p.type='api' AND p.code=? AND p.path=?",
                model.code(), CURRENT_MODEL_LIST)).isEqualTo(1);
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_role_permission rp JOIN t_permission p"
                        + " ON p.id=rp.permission_id WHERE rp.role_id=3 AND rp.del_flag=0"
                        + " AND p.del_flag=0 AND p.type='api' AND p.path=?",
                CURRENT_AGENT_LIST)).isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission WHERE type='api' AND path=? AND del_flag=0",
                LEGACY_AGENT_LIST)).isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission WHERE code='ai:model:read' AND type='button'"
                        + " AND status='active' AND del_flag=0")).isEqualTo(1);
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission WHERE code='ai:model:write' AND type='button'"
                        + " AND status='active' AND del_flag=0")).isEqualTo(1);
    }

    @Test
    void givesEveryAgentMenuItsOwnIconAndKeepsValidPermissionParents() throws Exception {
        var source = source();
        var jdbc = new JdbcTemplate(source);
        seedHistoricalState(source, jdbc);
        var migrations = new AgentSchemaMigrationService(source);
        migrations.migrateSchema();
        migrations.migratePermissions();
        var icons = jdbc.queryForList(
                "SELECT icon FROM t_menu WHERE path LIKE '/admin/agent-console%' AND del_flag=0",
                String.class);
        assertThat(icons).hasSize(9).doesNotContainNull().doesNotContain("").doesNotHaveDuplicates();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission p LEFT JOIN t_permission parent"
                        + " ON parent.id=p.parent_id AND parent.del_flag=0"
                        + " WHERE p.type='api' AND p.del_flag=0"
                        + " AND (p.parent_id IS NULL OR p.parent_id=0 OR parent.id IS NULL)"))
                .isZero();
        assertThat(count(jdbc,
                "SELECT COUNT(*) FROM t_permission p LEFT JOIN t_permission parent"
                        + " ON parent.id=p.parent_id AND parent.del_flag=0"
                        + " WHERE p.type='menu' AND p.del_flag=0"
                        + " AND (p.parent_id IS NULL OR (p.parent_id<>0 AND parent.id IS NULL))"))
                .isZero();
    }

    private static DriverManagerDataSource source() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE t_menu(id BIGINT AUTO_INCREMENT PRIMARY KEY,title VARCHAR(120),"
                + "name VARCHAR(120),path VARCHAR(200),component VARCHAR(120),icon VARCHAR(64),"
                + "parent_id BIGINT,sort INT,type VARCHAR(24),hidden INT,keep_alive INT,"
                + "requires_auth INT,status VARCHAR(24),permissions VARCHAR(200),"
                + "create_time DATETIME,update_time DATETIME,del_flag INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE t_permission(id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "name VARCHAR(120),code VARCHAR(120),type VARCHAR(24),parent_id BIGINT,"
                + "path VARCHAR(200),method VARCHAR(16),description VARCHAR(300),status VARCHAR(24),"
                + "sort INT,create_time DATETIME,update_time DATETIME,del_flag INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE t_role(id BIGINT PRIMARY KEY,name VARCHAR(120),code VARCHAR(64),"
                + "status VARCHAR(24),del_flag INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE t_role_menu(id BIGINT AUTO_INCREMENT PRIMARY KEY,role_id BIGINT,"
                + "menu_id BIGINT,create_time DATETIME,update_time DATETIME,del_flag INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE t_role_permission(id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "role_id BIGINT,permission_id BIGINT,create_time DATETIME,update_time DATETIME,"
                + "del_flag INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE t_user(id BIGINT PRIMARY KEY,username VARCHAR(64),"
                + "status VARCHAR(24),del_flag INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE t_user_role(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT,"
                + "role_id BIGINT,create_time DATETIME,update_time DATETIME,del_flag INT DEFAULT 0)");
        return source;
    }

    private static void seedHistoricalState(DriverManagerDataSource source, JdbcTemplate jdbc)
            throws Exception {
        jdbc.update("INSERT INTO t_role(id,name,code,status) VALUES"
                + " (1,'超级管理员','superAdmin','active'),(2,'普通管理员','admin','active'),"
                + " (3,'普通成员','member','active')");
        jdbc.update("INSERT INTO t_user(id,username,status) VALUES"
                + " (101,'admin','active'),(102,'ordinary-admin','active')");
        jdbc.update("INSERT INTO t_user_role(user_id,role_id) VALUES(101,2),(102,2)");
        jdbc.update("INSERT INTO t_menu(id,title,name,path,icon,status)"
                + " VALUES(900,'既有菜单','ExistingMenu','/admin/existing','House','active')");
        jdbc.update("INSERT INTO t_role_menu(role_id,menu_id) VALUES(2,900)");
        jdbc.update("INSERT INTO t_permission(id,name,code,type,parent_id,status)"
                + " VALUES(900,'既有按钮','system:existing:view','button',0,'active')");
        jdbc.update("INSERT INTO t_role_permission(role_id,permission_id) VALUES(2,900)");
        jdbc.update("INSERT INTO t_permission(id,name,code,type,parent_id,path,method,status)"
                + " VALUES(901,'旧版智能体列表','ai:agent:list:api','api',0,?,'GET','active')",
                LEGACY_AGENT_LIST);
        jdbc.update("INSERT INTO t_role_permission(role_id,permission_id) VALUES(3,901)");
        try (var connection = source.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new ClassPathResource("db/agent/V004__operations_permissions.sql"));
        }
        Long modelPermission = jdbc.queryForObject(
                "SELECT id FROM t_permission WHERE path=? AND method='GET' AND del_flag=0",
                Long.class, CURRENT_MODEL_LIST);
        jdbc.update("INSERT INTO t_role_permission(role_id,permission_id) VALUES(2,?)",
                modelPermission);
    }

    private static List<Map<String, Object>> rbacSnapshot(JdbcTemplate jdbc) {
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (String table : List.of("t_permission", "t_menu", "t_role_permission", "t_role_menu",
                "t_user_role")) {
            for (var row : jdbc.queryForList("SELECT * FROM " + table + " ORDER BY id")) {
                Map<String, Object> tagged = new LinkedHashMap<>(row);
                tagged.remove("CREATE_TIME");
                tagged.remove("UPDATE_TIME");
                tagged.put("TABLE", table);
                snapshot.add(tagged);
            }
        }
        return snapshot;
    }

    private static void assertSameSnapshot(
            List<Map<String, Object>> before, List<Map<String, Object>> after) {
        assertThat(after).hasSameSizeAs(before);
        for (int index = 0; index < before.size(); index++) {
            var row = before.get(index);
            assertThat(after.get(index)).as(row.get("TABLE") + "#" + row.get("ID"))
                    .isEqualTo(row);
        }
    }

    private static int count(JdbcTemplate jdbc, String sql, Object... arguments) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Integer.class, arguments));
    }

    private static List<Endpoint> catalog() throws Exception {
        JsonNode value = new ObjectMapper().readTree(new ClassPathResource(
                "rbac/permission-catalog.json").getInputStream());
        List<Endpoint> endpoints = new ArrayList<>();
        for (JsonNode item : value) endpoints.add(new Endpoint(
                item.path("method").asText(), item.path("path").asText(),
                item.path("name").asText(), item.path("code").asText(),
                item.path("parentCode").asText()));
        return endpoints;
    }

    private static Set<String> dynamicControllerEndpoints() throws Exception {
        var reader = new CachingMetadataReaderFactory();
        Set<String> keys = new HashSet<>();
        for (var resource : new PathMatchingResourcePatternResolver().getResources(
                "classpath*:com/iceeboot/framework/controller/**/*Controller.class")) {
            Class<?> controller = Class.forName(
                    reader.getMetadataReader(resource).getClassMetadata().getClassName());
            RequestMapping root = controller.getAnnotation(RequestMapping.class);
            if (root == null) continue;
            for (Method method : controller.getDeclaredMethods()) {
                RequirePermission permission = method.getAnnotation(RequirePermission.class);
                if (permission == null) continue;
                assertThat(permission.dynamic()).as(method.toString()).isTrue();
                String verb;
                String[] paths;
                if (method.isAnnotationPresent(GetMapping.class)) {
                    verb = "GET";
                    paths = method.getAnnotation(GetMapping.class).value();
                } else if (method.isAnnotationPresent(PostMapping.class)) {
                    verb = "POST";
                    paths = method.getAnnotation(PostMapping.class).value();
                } else if (method.isAnnotationPresent(PutMapping.class)) {
                    verb = "PUT";
                    paths = method.getAnnotation(PutMapping.class).value();
                } else if (method.isAnnotationPresent(DeleteMapping.class)) {
                    verb = "DELETE";
                    paths = method.getAnnotation(DeleteMapping.class).value();
                } else throw new AssertionError("缺少已支持的请求方法：" + method);
                for (String path : paths.length == 0 ? new String[] {""} : paths) {
                    assertThat(keys.add(verb + " " + root.value()[0] + path)).isTrue();
                }
            }
        }
        return keys;
    }

    private record Endpoint(String method, String path, String name, String code, String parentCode) {
        String key() { return method + " " + path; }
    }
}
