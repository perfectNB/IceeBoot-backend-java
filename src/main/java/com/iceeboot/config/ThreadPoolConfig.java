/**
 * 线程池配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置类
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {
    
    /**
     * 核心线程数
     */
    private static final int CORE_POOL_SIZE = 10;
    
    /**
     * 最大线程数
     */
    private static final int MAX_POOL_SIZE = 50;
    
    /**
     * 队列容量
     */
    private static final int QUEUE_CAPACITY = 200;
    
    /**
     * 线程空闲时间
     */
    private static final int KEEP_ALIVE_SECONDS = 60;
    
    /**
     * 异步任务执行线程池
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        executor.setCorePoolSize(CORE_POOL_SIZE);
        // 最大线程数
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        // 队列容量
        executor.setQueueCapacity(QUEUE_CAPACITY);
        // 线程空闲时间
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        // 线程名前缀
        executor.setThreadNamePrefix("IceeBoot-Async-");
        // 拒绝策略：由调用线程处理该任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 等待时间
        executor.setAwaitTerminationSeconds(60);
        // 初始化
        executor.initialize();
        return executor;
    }
    
    /**
     * AI模型调用专用线程池
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // AI调用通常耗时较长，设置较小的核心线程数
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("IceeBoot-AI-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}