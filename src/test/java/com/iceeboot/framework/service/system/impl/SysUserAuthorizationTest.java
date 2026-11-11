package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.iceeboot.common.exception.AuthServiceUnavailableException;
import com.iceeboot.framework.entity.system.RoleDO;
import com.iceeboot.framework.entity.system.UserRoleDO;
import com.iceeboot.framework.mapper.system.SysUserMapper;
import com.iceeboot.framework.service.database.DatabaseInitService;
import com.iceeboot.framework.service.system.IAuthCacheService;
import com.iceeboot.framework.service.system.ISysRoleService;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SysUserAuthorizationTest {
    @Test
    void businessPermissionLookupDelegatesToTheSameEffectivePermissionsAsTheInterceptor() {
        var mapper = mock(SysUserMapper.class);
        var auth = mock(IAuthCacheService.class);
        when(auth.getUserPermissions(7L)).thenReturn(List.of(
                Map.of("code", "system:user:list"), Map.of("code", "ai:agent:invoke")));
        var service = new SysUserServiceImpl(mapper, mock(ISysRoleService.class), auth,
                mock(RedisTemplate.class));
        assertThat(service.getUserPermissions(7L))
                .containsExactly("system:user:list", "ai:agent:invoke");
        verify(auth).getUserPermissions(7L);
        verifyNoInteractions(mapper);
    }

    @Test
    void failedEffectiveAuthorizationDoesNotFallBackToTheLegacyPermissionJoin() {
        var mapper = mock(SysUserMapper.class);
        var auth = mock(IAuthCacheService.class);
        var failure = new AuthServiceUnavailableException(new IllegalStateException("fixture"));
        when(auth.getUserPermissions(7L)).thenThrow(failure);
        var service = new SysUserServiceImpl(mapper, mock(ISysRoleService.class), auth,
                mock(RedisTemplate.class));
        assertThatThrownBy(() -> service.getUserPermissions(7L)).isSameAs(failure);
        verifyNoInteractions(mapper);
    }

    @Test
    void roleMapperReturnsOnlyCurrentActiveUndeletedRolesAndAssignments() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:user_roles_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(ds);
        for (var type : List.of(RoleDO.class, UserRoleDO.class)) {
            String table = type.getAnnotation(com.baomidou.mybatisplus.annotation.TableName.class).value();
            String ddl = ReflectionTestUtils.invokeMethod(new DatabaseInitService(null),
                    "generateCreateTableSql", type, table);
            jdbc.execute(Objects.requireNonNull(ddl).replaceAll("(?s) ENGINE=.*$", ""));
        }
        jdbc.update("INSERT INTO t_role(id,name,code,status,del_flag) VALUES"
                + " (42,'有效超级管理员','superAdmin','active',0),"
                + " (43,'禁用角色','disabled','inactive',0),"
                + " (44,'已删除角色','deleted','active',1),"
                + " (45,'已撤销关系','revoked','active',0),"
                + " (46,'普通管理角色','admin','active',0),"
                + " (47,'其他用户角色','other','active',0)");
        jdbc.update("INSERT INTO t_user_role(id,user_id,role_id,del_flag) VALUES"
                + " (90,7,42,0),(91,7,43,0),(92,7,44,0),(93,7,45,1),(94,7,46,0),(95,8,47,0)");
        var config = new MybatisConfiguration();
        config.addMapper(SysUserMapper.class);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds);
        factory.setConfiguration(config);
        factory.setMapperLocations(new ClassPathResource("mapper/system/SysUserMapper.xml"));
        var mapper = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()))
                .getMapper(SysUserMapper.class);
        var service = new SysUserServiceImpl(mapper, mock(ISysRoleService.class),
                mock(IAuthCacheService.class), mock(RedisTemplate.class));
        assertThat(service.getUserRoles(7L)).containsExactlyInAnyOrder("superAdmin", "admin");
        jdbc.update("UPDATE t_role SET status='inactive' WHERE id=42");
        assertThat(service.getUserRoles(7L)).containsExactly("admin");
        jdbc.update("UPDATE t_user_role SET del_flag=1 WHERE user_id=7 AND role_id=46");
        assertThat(service.getUserRoles(7L)).isEmpty();
    }
}
