/**
 * 数据库日志追加器
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.framework.appender;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.iceeboot.config.log.LogConfig;
import com.iceeboot.framework.entity.log.LogDO;
import com.iceeboot.framework.service.log.LogService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 数据库日志追加器
 * 用于将日志信息存储到数据库
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
public class DatabaseLogAppender extends AppenderBase<ILoggingEvent> {

    private static ApplicationContext applicationContext;
    private static LogService logService;
    private static LogConfig logConfig;
    private static volatile boolean isShuttingDown = false;
    private static final ExecutorService executor = Executors.newFixedThreadPool(5, r -> {
        Thread thread = new Thread(r, "DatabaseLogAppender-Worker");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * 设置Spring应用上下文
     */
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
        if (context != null) {
            try {
                logService = context.getBean(LogService.class);
                logConfig = context.getBean(LogConfig.class);
            } catch (Exception e) {
                // 忽略异常，可能是在启动过程中
            }
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        // 检查应用是否正在关闭
        if (isShuttingDown) {
            return;
        }
        
        // 检查是否已初始化
        if (logService == null || logConfig == null) {
            return;
        }

        // 检查是否启用日志存储
        if (!logConfig.isEnabled()) {
            return;
        }

        // 检查日志级别
        if (!shouldLog(event.getLevel().toString())) {
            return;
        }

        // 过滤系统内部日志
        String loggerName = event.getLoggerName();
        if (isSystemLog(loggerName)) {
            return;
        }

        // 异步处理日志
        executor.submit(() -> {
            try {
                // 再次检查关闭状态
                if (!isShuttingDown) {
                    saveLogToDatabase(event);
                }
            } catch (Exception e) {
                // 避免日志记录异常导致的循环
                if (!isShuttingDown) {
                    System.err.println("保存日志到数据库时发生异常: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 保存日志到数据库
     */
    private void saveLogToDatabase(ILoggingEvent event) {
        LogDO logDO = new LogDO();
        
        // 基本信息
        logDO.setLevel(event.getLevel().toString());
        logDO.setMessage(event.getFormattedMessage());
        logDO.setClassName(event.getLoggerName());
        logDO.setThread(event.getThreadName());
        logDO.setTimestamp(LocalDateTime.ofInstant(
            Instant.ofEpochMilli(event.getTimeStamp()), 
            ZoneId.systemDefault()
        ));

        // 获取模块信息
        logDO.setModule(getModuleName(event.getLoggerName()));

        // 获取请求信息
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logDO.setPath(request.getRequestURI());
                
                // 尝试获取Operation注解信息
                String summary = getOperationSummary(event.getLoggerName());
                logDO.setSummary(summary);
            }
        } catch (Exception e) {
            // 忽略获取请求信息的异常
        }

        // 异常信息
        if (event.getThrowableProxy() != null) {
            logDO.setStackTrace(event.getThrowableProxy().getMessage());
        }

        // 保存到数据库
        logService.saveLog(logDO);
    }

    /**
     * 检查是否应该记录该级别的日志
     */
    private boolean shouldLog(String eventLevel) {
        String configLevel = logConfig.getLevel().toUpperCase();
        
        // 定义日志级别优先级
        int eventPriority = getLogLevelPriority(eventLevel);
        int configPriority = getLogLevelPriority(configLevel);
        
        return eventPriority >= configPriority;
    }

    /**
     * 获取日志级别优先级
     */
    private int getLogLevelPriority(String level) {
        switch (level.toUpperCase()) {
            case "DEBUG": return 1;
            case "INFO": return 2;
            case "WARN": return 3;
            case "ERROR": return 4;
            default: return 0;
        }
    }

    /**
     * 判断是否为系统内部日志
     */
    private boolean isSystemLog(String loggerName) {
        // 排除框架和系统日志
        String[] excludePatterns = {
            "org.springframework",
            "org.apache",
            "com.baomidou.mybatisplus",
            "org.mybatis",
            "com.zaxxer.hikari",
            "org.hibernate",
            "ch.qos.logback",
            "com.iceeboot.framework.appender", // 排除自己
            "com.iceeboot.framework.mapper", // 排除MyBatis日志
            "root"
        };
        
        for (String pattern : excludePatterns) {
            if (loggerName.startsWith(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 根据类名获取模块名
     */
    private String getModuleName(String className) {
        if (className.contains("system") || className.contains("System")) {
            return "系统管理";
        } else if (className.contains("redis") || className.contains("Redis")) {
            return "Redis管理";
        } else if (className.contains("log") || className.contains("Log")) {
            return "日志管理";
        } else if (className.contains("dashboard") || className.contains("Dashboard")) {
            return "仪表盘";
        } else if (className.contains("task") || className.contains("Task")) {
            return "任务管理";
        } else if (className.contains("workflow") || className.contains("Workflow")) {
            return "工作流";
        } else if (className.contains("ai") || className.contains("Ai")) {
            return "AI模块";
        } else {
            return "其他";
        }
    }

    /**
     * 尝试获取Operation注解的summary信息
     */
    private String getOperationSummary(String className) {
        try {
            // 通过堆栈跟踪找到Controller方法
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            
            for (StackTraceElement element : stackTrace) {
                String elementClassName = element.getClassName();
                
                // 查找Controller类
                if (elementClassName.contains("Controller")) {
                    try {
                        Class<?> clazz = Class.forName(elementClassName);
                        Method[] methods = clazz.getDeclaredMethods();
                        
                        for (Method method : methods) {
                            if (method.getName().equals(element.getMethodName())) {
                                Operation operation = method.getAnnotation(Operation.class);
                                if (operation != null) {
                                    return operation.summary();
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 忽略反射异常
                    }
                }
            }
            
            return "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 标记应用正在关闭
     */
    public static void markShuttingDown() {
        isShuttingDown = true;
    }
    
    /**
     * 优雅关闭线程池
     */
    public static void gracefulShutdown() {
        markShuttingDown();
        if (executor != null && !executor.isShutdown()) {
            try {
                // 停止接收新任务
                executor.shutdown();
                // 等待现有任务完成，最多等待5秒
                if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    // 强制关闭
                    executor.shutdownNow();
                    // 再等待2秒
                    if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                        System.err.println("DatabaseLogAppender线程池无法正常关闭");
                    }
                }
            } catch (InterruptedException e) {
                // 强制关闭
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        gracefulShutdown();
    }
}