/**
 * Knife4j配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.config;

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
                        .title("ZBoot超级脚手架API文档")
                        .version("1.0.0")
                        .description("ZBoot超级脚手架项目的API接口文档")
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
                .pathsToMatch("/zboot/auth/**")
                .build();
    }
    
    /**
     * 用户模块API分组
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户模块")
                .pathsToMatch("/zboot/user/**")
                .build();
    }
    
    /**
     * 系统模块API分组
     */
    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("系统模块")
                .pathsToMatch("/zboot/system/**")
                .build();
    }
    
    /**
     * AI模块API分组
     */
    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
                .group("AI模块")
                .pathsToMatch("/zboot/ai/**")
                .build();
    }
    
    /**
     * 工作流模块API分组
     */
    @Bean
    public GroupedOpenApi workflowApi() {
        return GroupedOpenApi.builder()
                .group("工作流模块")
                .pathsToMatch("/zboot/workflow/**")
                .build();
    }
    
    /**
     * 所有API分组
     */
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("全部接口")
                .pathsToMatch("/zboot/**")
                .build();
    }
}