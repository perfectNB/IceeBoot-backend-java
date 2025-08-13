/**
 * MyBatis-Plus配置类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import com.zboot.common.utils.TokenUtils;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus配置类
 */
@Configuration
public class MybatisPlusConfig {
    
    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
    
    /**
     * 自动填充处理器
     */
    @Component
    public static class MyMetaObjectHandler implements MetaObjectHandler {
        
        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
            
            // 获取当前用户ID，这里暂时设置为系统，后续可以从Sa-Token中获取
            String currentUserId = getCurrentUserId();
            this.strictInsertFill(metaObject, "createBy", String.class, currentUserId);
            this.strictInsertFill(metaObject, "updateBy", String.class, currentUserId);
            
            // 设置删除标志为未删除
            this.strictInsertFill(metaObject, "delFlag", Integer.class, 0);
        }
        
        @Override
        public void updateFill(MetaObject metaObject) {
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            
            // 获取当前用户ID
            String currentUserId = getCurrentUserId();
            this.strictUpdateFill(metaObject, "updateBy", String.class, currentUserId);
        }
        
        /**
         * 获取当前用户ID
         * 从TokenUtils获取当前登录用户ID
         */
        private String getCurrentUserId() {
            try {
                // 从TokenUtils获取当前登录用户ID
                String userId = TokenUtils.getCurrentUserIdAsString();
                return userId != null ? userId : "system";
            } catch (Exception e) {
                // 如果获取失败，返回系统用户
                return "system";
            }
        }
    }
}