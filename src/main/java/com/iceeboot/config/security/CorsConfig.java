package com.iceeboot.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 * 
 * @author CodeIcee
 * @date 2025-08-13
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("#{'${iceeboot.cors.allowed-origins:http://localhost:5173,http://localhost:5137}'.split(',')}")
    private String[] allowedOrigins;

    @Value("#{'${iceeboot.cors.allowed-headers:*}'.split(',')}")
    private String[] allowedHeaders;

    @Value("#{'${iceeboot.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}'.split(',')}")
    private String[] allowedMethods;

    @Value("${iceeboot.cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${iceeboot.cors.max-age:3600}")
    private long maxAge;

    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods)
                .allowCredentials(allowCredentials)
                .maxAge(maxAge);
    }
}
