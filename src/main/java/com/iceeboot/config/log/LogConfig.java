/**
 * 日志配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.config.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 日志配置类
 * 用于读取日志存储相关配置
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
@Data
@Component
@ConfigurationProperties(prefix = "iceeboot.log")
public class LogConfig {

    /**
     * 是否启用日志存储
     */
    private boolean enabled = true;

    /**
     * 日志存储级别
     * 可选值：DEBUG, INFO, WARN, ERROR
     */
    private String level = "INFO";

    /**
     * 是否异步存储
     */
    private boolean async = true;

    /**
     * 日志保留天数
     */
    private int retentionDays = 30;

    /**
     * 批量保存大小
     */
    private int batchSize = 100;

    /**
     * 是否记录请求参数
     */
    private boolean logRequestParams = false;

    /**
     * 是否记录响应结果
     */
    private boolean logResponseResult = false;

    /**
     * 排除的路径模式
     */
    private String[] excludePatterns = {"/actuator/**", "/swagger-ui/**", "/v3/api-docs/**"};
}