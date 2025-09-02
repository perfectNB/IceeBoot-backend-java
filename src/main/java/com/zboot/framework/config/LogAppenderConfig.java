/**
 * 日志追加器配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.framework.config;

import ch.qos.logback.classic.LoggerContext;
import com.zboot.framework.appender.DatabaseLogAppender;
import lombok.RequiredArgsConstructor;
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

            System.out.println("数据库日志追加器初始化成功");
        } catch (Exception e) {
            System.err.println("初始化数据库日志追加器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}