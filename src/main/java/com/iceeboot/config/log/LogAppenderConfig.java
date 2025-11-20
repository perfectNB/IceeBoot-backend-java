/**
 * 日志追加器配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.config.log;

import ch.qos.logback.classic.LoggerContext;
import com.iceeboot.framework.appender.DatabaseLogAppender;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 日志追加器配置类
 * 用于配置和初始化数据库日志追加器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogAppenderConfig {

    private final ApplicationContext applicationContext;
    private final LogConfig logConfig;

    /**
     * 应用启动完成后初始化日志追加器
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initLogAppender() {
        if (!logConfig.isEnabled()) {
            return;
        }

        try {
            // 设置ApplicationContext到DatabaseLogAppender
            DatabaseLogAppender.setApplicationContext(applicationContext);

            // 获取Logback的LoggerContext
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            
            // 创建DatabaseLogAppender实例
            DatabaseLogAppender databaseAppender = new DatabaseLogAppender();
            databaseAppender.setContext(loggerContext);
            databaseAppender.setName("DATABASE");
            databaseAppender.start();

            // 将appender添加到root logger
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            rootLogger.addAppender(databaseAppender);

            log.info("数据库日志追加器初始化成功");
        } catch (Exception e) {
            log.error("初始化数据库日志追加器失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 应用关闭时的清理工作
     */
    @PreDestroy
    public void destroy() {
        log.info("LogAppenderConfig正在执行清理工作...");
        try {
            DatabaseLogAppender.gracefulShutdown();
            log.info("数据库日志追加器清理完成");
        } catch (Exception e) {
            log.error("清理数据库日志追加器时发生异常: {}", e.getMessage(), e);
        }
    }
}