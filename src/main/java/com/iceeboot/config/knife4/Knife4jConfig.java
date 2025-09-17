/**
 * Knife4j配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.config.knife4;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j配置类
 * @author CodeIcee
 * @date 2025-08-11
 */
@Configuration
public class Knife4jConfig {
    
    /**
     * 创建API文档信息
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("IceeBoot超级脚手架API文档")
                        .version("1.0.0")
                        .description("IceeBoot超级脚手架项目的API接口文档")
                        .contact(new Contact()
                                .name("CodeIcee")
                                .email("codeicee@example.com")
                                .url("https://github.com/codeicee"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
    
    /**
     * 认证模块API分组
     */
    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("认证模块")
                .pathsToMatch("/iceeboot/api/v1/auth/**", "/v1/auth/**")
                .build();
    }
    
    /**
     * 用户模块API分组
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户模块")
                .pathsToMatch("/iceeboot/api/v1/users/**", "/v1/users/**")
                .build();
    }
    
    /**
     * 系统模块API分组
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统模块")
                .pathsToMatch("/iceeboot/api/v1/permissions/**", "/v1/permissions/**", 
                             "/iceeboot/api/v1/roles/**", "/v1/roles/**",
                             "/iceeboot/api/v1/permission-test/**", "/v1/permission-test/**")
                .build();
    }
    
    /**
     * AI模块API分组
     */
    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("AI模块")
                .pathsToMatch("/iceeboot/api/v1/agents/**", "/v1/agents/**",
                             "/iceeboot/api/v1/chat/**", "/v1/chat/**",
                             "/iceeboot/api/v1/llm/**", "/v1/llm/**")
                .build();
    }
    
    /**
     * 工作流模块API分组
     */
    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
                .group("工作流模块")
                .pathsToMatch("/iceeboot/api/api/v1/workflow/**", "/api/v1/workflow/**")
                .build();
    }
    
    /**
     * 任务模块API分组
     */
    @Bean
    public GroupedOpenApi taskApi() {
        return GroupedOpenApi.builder()
                .group("任务模块")
                .pathsToMatch("/iceeboot/api/v1/tasks/**", "/v1/tasks/**")
                .build();
    }
    
    /**
     * 仪表盘模块API分组
     */
    @Bean
    public GroupedOpenApi dashboardApi() {
        return GroupedOpenApi.builder()
                .group("仪表盘模块")
                .pathsToMatch("/iceeboot/api/v1/dashboard/**", "/v1/dashboard/**")
                .build();
    }
    
    /**
     * Redis模块API分组
     */
    @Bean
    public GroupedOpenApi redisApi() {
        return GroupedOpenApi.builder()
                .group("Redis模块")
                .pathsToMatch("/iceeboot/api/v1/redis/**", "/v1/redis/**")
                .build();
    }
    
    /**
     * 所有API分组
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/iceeboot/api/**", "/v1/**", "/api/v1/**")
                .packagesToScan("com.iceeboot.framework.controller")
                .build();
    }
}