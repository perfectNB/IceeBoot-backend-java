/**
 * 安全配置类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
package com.zboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 安全配置类
 * 
 * @author CodeIcee
 * @date 2025-01-11
 */
@Configuration
public class SecurityConfig {

    /**
     * 密码加密器
     * 
     * @return BCryptPasswordEncoder
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}