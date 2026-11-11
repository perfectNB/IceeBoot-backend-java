package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.framework.entity.system.MenuDO;
import com.iceeboot.framework.entity.system.PermissionDO;
import com.iceeboot.framework.mapper.system.SysMenuMapper;
import com.iceeboot.framework.mapper.system.SysPermissionMapper;
import com.iceeboot.framework.service.database.DatabaseInitService;
import com.iceeboot.framework.service.system.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 模拟当前环境逻辑删除字段与实体字段不一致，验证授权弹框仍明确排除已删除资源。 */
class RoleAuthorizationTreeTest {
    SysPermissionServiceImpl permissions;
    SysMenuServiceImpl menus;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:grant_trees_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        var init = new DatabaseInitService(null);
        for (var entity : List.of(PermissionDO.class, MenuDO.class)) {
            String table = entity == PermissionDO.class ? "t_permission" : "t_menu";
            String ddl = ReflectionTestUtils.invokeMethod(init, "generateCreateTableSql", entity, table);
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
            jdbc.update("INSERT INTO " + table + "(id,name,status,parent_id,sort,del_flag) VALUES"
                    + "(1,'active-root','active',0,1,0),(2,'deleted-root','active',0,2,1),"
                    + "(3,'disabled-root','inactive',0,3,0),(4,'active-child','active',1,1,0)");
        }
        var cfg = new MybatisConfiguration();
        cfg.addMapper(SysPermissionMapper.class); cfg.addMapper(SysMenuMapper.class);
        var global = new GlobalConfig();
        global.setDbConfig(new GlobalConfig.DbConfig().setLogicDeleteField("deleted"));
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds); factory.setConfiguration(cfg); factory.setGlobalConfig(global);
        var sql = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        var rolePermissions = mock(ISysRolePermissionService.class);
        when(rolePermissions.getPermissionIdsByRoleId(7L)).thenReturn(List.of(1L,4L));
        var roleMenus = mock(ISysRoleMenuService.class);
        when(roleMenus.getMenuIdsByRoleId(7L)).thenReturn(List.of(1L,4L));
        permissions = new SysPermissionServiceImpl(rolePermissions,mock(RedisTemplate.class));
        ReflectionTestUtils.setField(permissions,"baseMapper",sql.getMapper(SysPermissionMapper.class));
        menus = new SysMenuServiceImpl(mock(IAuthCacheService.class),roleMenus,mock(RedisTemplate.class));
        ReflectionTestUtils.setField(menus,"baseMapper",sql.getMapper(SysMenuMapper.class));
    }

    @Test void permissionSelectionExcludesDeletedResourcesAndPreservesExistingStatusSemantics() {
        var tree = permissions.getPermissionTreeByRoleId(7L);
        assertThat(tree).extracting(node -> node.getId()).containsExactlyInAnyOrder(1L,3L);
        var root = tree.stream().filter(node -> node.getId()==1L).findFirst().orElseThrow();
        assertThat(root.getHasPermission()).isTrue();
        assertThat(root.getChildren()).extracting(node -> node.getId()).containsExactly(4L);
        assertThat(root.getChildren().getFirst().getHasPermission()).isTrue();
    }

    @Test void menuSelectionExcludesDeletedAndInactiveResourcesAndPreservesChildren() {
        var tree = menus.getMenuTreeByRoleId(7L);
        assertThat(tree).extracting(node -> node.getId()).containsExactly(1L);
        assertThat(tree.getFirst().getHasPermission()).isTrue();
        assertThat(tree.getFirst().getChildren()).extracting(node -> node.getId()).containsExactly(4L);
        assertThat(tree.getFirst().getChildren().getFirst().getHasPermission()).isTrue();
    }
}
