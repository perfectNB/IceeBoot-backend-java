/**
 * Function Calling 配置类
 * 注册数据库查询函数供 Spring AI Alibaba 调用
 * 
 * @author CodeIcee
 * @date 2025-09-10
 */
package com.iceeboot.framework.config;

import com.iceeboot.framework.llm.model.DatabaseQueryRequest;
import com.iceeboot.framework.llm.model.DatabaseQueryResponse;
import com.iceeboot.framework.llm.service.DatabaseQueryFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Function Calling 配置类
 */
@Configuration
public class FunctionCallingConfig {

    /**
     * 注册数据库查询函数
     */
    @Bean
    @Description("查询数据库获取信息，支持用户、角色、权限、任务、活动、文件等数据的查询。可以根据查询意图自动选择相关表进行查询。")
    public Function<DatabaseQueryRequest, DatabaseQueryResponse> databaseQuery(
            DatabaseQueryFunction databaseQueryFunction) {
        return databaseQueryFunction;
    }
}