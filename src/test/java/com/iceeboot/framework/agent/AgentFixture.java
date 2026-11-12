package com.iceeboot.framework.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.config.agent.AgentProperties;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.IAgentConfigurationService;
import com.iceeboot.framework.service.agent.IAgentPersistenceService;
import com.iceeboot.framework.service.agent.security.AgentSecrets;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.*;
import java.util.*;

class AgentFixture {
    final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    final AgentProperties properties = new AgentProperties();
    final JdbcTemplate jdbc;
    private final org.mybatis.spring.SqlSessionTemplate session;
    final IAgentPersistenceService store;
    final AgentSecrets secrets;
    final IAgentConfigurationService configs;

    AgentFixture() throws Exception {
        var data =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                        "sa",
                        "");
        jdbc = new JdbcTemplate(data);
        String ddl = Files.readString(Path.of("src/main/resources/sql/agentscope_schema.sql"));
        for (String statement : ddl.split(";")) if (!statement.isBlank()) jdbc.execute(statement);
        properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        properties.setWorkspace(Files.createTempDirectory("iceeboot-agent-test-").toString());
        var configuration = new com.baomidou.mybatisplus.core.MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        var globals = new com.baomidou.mybatisplus.core.config.GlobalConfig();
        globals.setDbConfig(
                new com.baomidou.mybatisplus.core.config.GlobalConfig.DbConfig()
                        .setLogicDeleteField("delFlag")
                        .setLogicDeleteValue("1")
                        .setLogicNotDeleteValue("0"));
        globals.setMetaObjectHandler(
                new com.iceeboot.config.mybatisplus.MybatisPlusConfig.MyMetaObjectHandler());
        var factory = new com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean();
        factory.setDataSource(data);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(globals);
        factory.setMapperLocations(
                new org.springframework.core.io.support.PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/agent/*.xml"));
        var sessionFactory = factory.getObject();
        for (Class<?> mapper :
                java.util.List.of(
                        com.iceeboot.framework.mapper.agent.AgentFileMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentConversationMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentSecretMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentUsageQuotaMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentModelMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentCapabilityMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentSkillMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentToolMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentMcpMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentSandboxTemplateMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentKnowledgeMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentDefinitionMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentReleaseMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentRunMapper.class,
                        com.iceeboot.framework.mapper.agent.AgentModelCallMapper.class)) {
            if (!sessionFactory.getConfiguration().hasMapper(mapper))
                sessionFactory.getConfiguration().addMapper(mapper);
        }
        session = new org.mybatis.spring.SqlSessionTemplate(sessionFactory);
        var codec = new com.iceeboot.framework.service.agent.support.AgentJsonCodec(json);
        var transactionManager =
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(data);
        var model =
                transactional(
                        new com.iceeboot.framework.service.agent.impl.AgentModelStorageServiceImpl(
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentModelMapper.class),
                                codec),
                        transactionManager);
        var resource =
                transactional(
                        new com.iceeboot.framework.service.agent.impl
                                .AgentResourceStorageServiceImpl(
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentCapabilityMapper
                                                .class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentSkillMapper.class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentToolMapper.class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentMcpMapper.class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent
                                                .AgentSandboxTemplateMapper.class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentKnowledgeMapper
                                                .class),
                                codec),
                        transactionManager);
        var definition =
                transactional(
                        new com.iceeboot.framework.service.agent.impl
                                .AgentDefinitionStorageServiceImpl(
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentDefinitionMapper
                                                .class),
                                codec),
                        transactionManager);
        var release =
                transactional(
                        new com.iceeboot.framework.service.agent.impl
                                .AgentReleaseStorageServiceImpl(
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentDefinitionMapper
                                                .class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentReleaseMapper
                                                .class),
                                codec),
                        transactionManager);
        var run =
                transactional(
                        new com.iceeboot.framework.service.agent.impl.AgentRunStorageServiceImpl(
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentRunMapper.class),
                                session.getMapper(
                                        com.iceeboot.framework.mapper.agent.AgentModelCallMapper
                                                .class),
                                codec),
                        transactionManager);
        var target =
                new com.iceeboot.framework.service.agent.impl.AgentPersistenceServiceImpl(
                        model, resource, definition, release, run, codec);
        var proxy = new org.springframework.aop.framework.ProxyFactory(target);
        proxy.addAdvice(
                new org.springframework.transaction.interceptor.TransactionInterceptor(
                        transactionManager,
                        new org.springframework.transaction.annotation
                                .AnnotationTransactionAttributeSource()));
        store = (IAgentPersistenceService) proxy.getProxy();
        secrets = new AgentSecrets(properties);
        var configTarget =
                new com.iceeboot.framework.service.agent.impl.AgentConfigurationServiceImpl(
                        store, secrets);
        var configProxy = new org.springframework.aop.framework.ProxyFactory(configTarget);
        configProxy.addAdvice(
                new org.springframework.transaction.interceptor.TransactionInterceptor(
                        transactionManager,
                        new org.springframework.transaction.annotation
                                .AnnotationTransactionAttributeSource()));
        configs = (IAgentConfigurationService) configProxy.getProxy();
    }

    private static <T> T transactional(
            T target, org.springframework.transaction.PlatformTransactionManager manager) {
        var proxy = new org.springframework.aop.framework.ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(
                new org.springframework.transaction.interceptor.TransactionInterceptor(
                        manager,
                        new org.springframework.transaction.annotation
                                .AnnotationTransactionAttributeSource()));
        return (T) proxy.getProxy();
    }

    com.iceeboot.framework.service.agent.IAgentConversationService conversations() {
        return transactional(new com.iceeboot.framework.service.agent.impl.AgentConversationServiceImpl(
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentConversationMapper.class),properties,json,secrets),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
    }

    com.iceeboot.framework.service.agent.IAgentFileService files() {
        return transactional(new com.iceeboot.framework.service.agent.impl.AgentFileServiceImpl(
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentFileMapper.class),conversations(),store),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
    }
    com.iceeboot.framework.service.agent.IAgentRecoveryService recovery() {
        return transactional(new com.iceeboot.framework.service.agent.impl.AgentRecoveryServiceImpl(conversations(),store,files(),
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentConversationMapper.class),
                new com.iceeboot.framework.service.agent.convert.AgentApiConverter(json),json),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
    }

    com.iceeboot.framework.service.agent.IAgentQuotaService quota() {
        return transactional(
                new com.iceeboot.framework.service.agent.impl.AgentQuotaServiceImpl(
                        session.getMapper(
                                com.iceeboot.framework.mapper.agent.AgentUsageQuotaMapper.class),
                        properties),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()));
    }

    com.iceeboot.framework.service.agent.IAgentSecretRotationService rotation() {
        return transactional(
                new com.iceeboot.framework.service.agent.impl.AgentSecretRotationServiceImpl(
                        session.getMapper(
                                com.iceeboot.framework.mapper.agent.AgentSecretMapper.class),
                        secrets,
                        properties,
                        json),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(
                        jdbc.getDataSource()));
    }

    private static <T> T entityService(T service, Object mapper) {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    com.iceeboot.framework.service.agent.IAgentSkillImportService skillImports() {
        return transactional(new com.iceeboot.framework.service.agent.impl.AgentSkillImportServiceImpl(
                new com.iceeboot.framework.service.agent.support.SkillArchiveParser(),
                new com.iceeboot.framework.service.agent.support.GithubSkillSource(),skillsService()),
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
    }

    com.iceeboot.framework.service.agent.IAgentSkillService skillsService() {
        return entityService(
                new com.iceeboot.framework.service.agent.impl.AgentSkillServiceImpl(
                        configs,
                        store,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(json)),
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentSkillMapper.class));
    }

    com.iceeboot.framework.service.agent.IAgentToolService toolsService() {
        return entityService(
                new com.iceeboot.framework.service.agent.impl.AgentToolServiceImpl(
                        configs,
                        store,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(json)),
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentToolMapper.class));
    }

    com.iceeboot.framework.service.agent.IAgentMcpService mcpsService() {
        return entityService(
                new com.iceeboot.framework.service.agent.impl.AgentMcpServiceImpl(
                        configs,
                        store,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(json)),
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentMcpMapper.class));
    }

    com.iceeboot.framework.service.agent.IAgentSandboxTemplateService sandboxesService() {
        return entityService(
                new com.iceeboot.framework.service.agent.impl.AgentSandboxTemplateServiceImpl(
                        configs,
                        store,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(json)),
                session.getMapper(
                        com.iceeboot.framework.mapper.agent.AgentSandboxTemplateMapper.class));
    }

    com.iceeboot.framework.service.agent.IAgentKnowledgeService knowledgeService() {
        return entityService(
                new com.iceeboot.framework.service.agent.impl.AgentKnowledgeServiceImpl(
                        configs,
                        store,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(json)),
                session.getMapper(com.iceeboot.framework.mapper.agent.AgentKnowledgeMapper.class));
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
        return configs.saveAgent(
                draft(code, model, skills, tools, mcps, children, sandbox, template));
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
