package com.iceeboot.framework.config;

import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Activiti工作流配置
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Configuration
public class ActivitiConfig {

    @Autowired
    private DataSource dataSource;

    /**
     * 流程引擎配置
     */
    @Bean
    public ProcessEngineConfiguration processEngineConfiguration() {
        StandaloneProcessEngineConfiguration configuration = new StandaloneProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);
        configuration.setAsyncExecutorActivate(false);
        configuration.setMailServerHost("localhost");
        configuration.setMailServerPort(25);
        return configuration;
    }

    /**
     * 流程引擎
     */
    @Bean
    public ProcessEngine processEngine() {
        return processEngineConfiguration().buildProcessEngine();
    }

    /**
     * 运行时服务
     */
    @Bean
    public RuntimeService runtimeService() {
        return processEngine().getRuntimeService();
    }

    /**
     * 任务服务
     */
    @Bean
    public TaskService taskService() {
        return processEngine().getTaskService();
    }

    /**
     * 历史服务
     */
    @Bean
    public HistoryService historyService() {
        return processEngine().getHistoryService();
    }

    /**
     * 仓库服务
     */
    @Bean
    public RepositoryService repositoryService() {
        return processEngine().getRepositoryService();
    }

    /**
     * 管理服务
     */
    @Bean
    public ManagementService managementService() {
        return processEngine().getManagementService();
    }
}