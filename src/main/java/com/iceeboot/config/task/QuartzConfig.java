package com.iceeboot.config.task;

import org.quartz.Scheduler;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Quartz配置类
 * 
 * @author CodeIcee
 * @date 2025-08-12
 */
@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource,
                                                       AutowireCapableBeanFactory beanFactory) {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setJobFactory((bundle, scheduler) ->
                beanFactory.createBean(bundle.getJobDetail().getJobClass()));
        factory.setApplicationContextSchedulerContextKey("applicationContext");
        factory.setQuartzProperties(quartzProperties());
        factory.setStartupDelay(1);
        factory.setAutoStartup(true);
        factory.setOverwriteExistingJobs(true);
        factory.setWaitForJobsToCompleteOnShutdown(true);
        return factory;
    }

    @Bean
    @Primary
    public Scheduler scheduler(SchedulerFactoryBean schedulerFactoryBean) {
        return schedulerFactoryBean.getScheduler();
    }

    private Properties quartzProperties() {
        Properties properties = new Properties();
        properties.put("org.quartz.scheduler.instanceName", "iceebootScheduler");
        properties.put("org.quartz.scheduler.instanceId", "AUTO");
        properties.put("org.quartz.threadPool.threadCount", "10");
        properties.put("org.quartz.threadPool.threadPriority", "5");
        properties.put("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
        // 使用内存存储
        properties.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        return properties;
    }
}
