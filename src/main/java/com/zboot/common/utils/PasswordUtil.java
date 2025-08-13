package com.zboot.common.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {
    // 创建加密器（可自定义强度，默认10）
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码（含盐值）
     */
    public static String encrypt(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     * @param rawPassword 用户输入的密码
     * @param encodedPassword 数据库存储的加密密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}