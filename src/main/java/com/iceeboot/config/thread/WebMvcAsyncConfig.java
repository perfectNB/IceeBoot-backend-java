package com.iceeboot.config.thread;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 异步执行器配置，避免 SSE 走默认的 SimpleAsyncTaskExecutor。
 */
@Component
public class WebMvcAsyncConfig implements WebMvcConfigurer {

    private final ThreadPoolTaskExecutor taskExecutor;

    public WebMvcAsyncConfig(@Qualifier("taskExecutor") ThreadPoolTaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setTaskExecutor(taskExecutor);
        configurer.setDefaultTimeout(120_000L);
    }
}
