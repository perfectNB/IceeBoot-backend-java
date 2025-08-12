/*
 * @Description: ZBoot超级脚手架启动类
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.zboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * ZBoot超级脚手架启动类
 * 
 * @author CodeIcee
 * @since 2025-08-11
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@EnableConfigurationProperties
public class ZbootApplication {

    public static void main(String[] args) {
        // 设置启动动画
        System.setProperty("spring.banner.location", "classpath:banner.txt");
        SpringApplication.run(ZbootApplication.class, args);

    }
}