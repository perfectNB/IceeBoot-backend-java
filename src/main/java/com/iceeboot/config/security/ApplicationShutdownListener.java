/**
 * 应用关闭监听器
 * 用于处理应用关闭时的清理工作
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.config.security;

import com.iceeboot.framework.appender.DatabaseLogAppender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

/**
 * 应用关闭监听器
 * 在Spring容器关闭时执行清理工作
 * 优先级设置为最高，确保在其他Bean销毁前执行
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
@Component
public class ApplicationShutdownListener implements ApplicationListener<ContextClosedEvent>, Ordered {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("应用正在关闭，开始执行清理工作...");
        
        try {
            // 优雅关闭数据库日志追加器
            log.info("正在关闭数据库日志追加器...");
            DatabaseLogAppender.gracefulShutdown();
            log.info("数据库日志追加器已关闭");
            
        } catch (Exception e) {
            // 使用System.err而不是log，避免在关闭过程中产生新的日志
            System.err.println("关闭数据库日志追加器时发生异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        log.info("应用清理工作完成");
    }

    /**
     * 设置最高优先级，确保在数据库连接池等其他资源关闭前执行
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}