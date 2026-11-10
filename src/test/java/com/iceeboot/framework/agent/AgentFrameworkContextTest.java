package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.config.mybatisplus.MybatisPlusConfig;
import com.iceeboot.framework.dto.create.agent.AgentSkillDTO;
import com.iceeboot.framework.service.agent.IAgentSkillService;
import com.iceeboot.framework.service.system.ISysUserService;

import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import javax.sql.DataSource;

/** 验证真实组件扫描、泛型 Mapper 注入及事务服务依赖，避免移包后仅独立单测能运行。 */
class AgentFrameworkContextTest {
    @Configuration
    @EnableTransactionManagement
    @MapperScan("com.iceeboot.framework.mapper.agent")
    @ComponentScan(
            basePackages = {
                "com.iceeboot.config.agent",
                "com.iceeboot.framework.service.agent",
                "com.iceeboot.framework.controller.agent"
            })
    static class ConfigurationFixture {
        @Bean
        DataSource dataSource() throws Exception {
            var data =
                    new DriverManagerDataSource(
                            "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                            "sa",
                            "");
            var jdbc = new JdbcTemplate(data);
            for (String sql :
                    Files.readString(Path.of("src/main/resources/sql/agentscope_schema.sql"))
                            .split(";")) {
                if (!sql.isBlank()) jdbc.execute(sql);
            }
            return data;
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }

        @Bean
        ISysUserService users() {
            return mock(ISysUserService.class);
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource source) {
            return new DataSourceTransactionManager(source);
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            var factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            var config = new MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(config);
            var global = new GlobalConfig();
            global.setDbConfig(
                    new GlobalConfig.DbConfig()
                            .setLogicDeleteField("delFlag")
                            .setLogicDeleteValue("1")
                            .setLogicNotDeleteValue("0"));
            global.setMetaObjectHandler(new MybatisPlusConfig.MyMetaObjectHandler());
            factory.setGlobalConfig(global);
            factory.setMapperLocations(
                    new PathMatchingResourcePatternResolver()
                            .getResources("classpath*:mapper/agent/*.xml"));
            return factory.getObject();
        }
    }

    @Test
    void frameworkComponentsStartAndInjectTypedMappersWithoutCircularDependencies() {
        try (var context = new AnnotationConfigApplicationContext(ConfigurationFixture.class)) {
            IAgentSkillService service = context.getBean(IAgentSkillService.class);
            var dto = new AgentSkillDTO();
            dto.setCode("context_skill");
            dto.setName("扫描测试");
            dto.setEnabled(true);
            dto.setContent("analyze");
            var saved = service.addResource(dto);
            assertThat(saved.getId()).isPositive();
            assertThat(saved.getCreateTime()).isNotNull();
            assertThat(service.getResourceList())
                    .extracting(value -> value.getId())
                    .containsExactly(saved.getId());
            service.deleteResource(saved.getId());
            assertThat(service.getResourceList()).isEmpty();
        }
    }
}
