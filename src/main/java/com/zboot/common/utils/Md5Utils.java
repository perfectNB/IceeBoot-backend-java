/**
 * MD5加密工具类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.common.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * MD5加密工具类
 */
@Slf4j
public class Md5Utils {
    
    /**
     * 默认盐值长度
     */
    private static final int DEFAULT_SALT_LENGTH = 16;
    
    /**
     * MD5加密（无盐值）
     * 
     * @param text 待加密文本
     * @return MD5加密后的字符串
     */
    public static String encrypt(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        return DigestUtil.md5Hex(text);
    }
    
    /**
     * MD5加密（带盐值）
     * 
     * @param text 待加密文本
     * @param salt 盐值
     * @return MD5加密后的字符串
     */
    public static String encrypt(String text, String salt) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (StrUtil.isBlank(salt)) {
            return encrypt(text);
        }
        return DigestUtil.md5Hex(text + salt);
    }
    
    /**
     * 生成随机盐值
     * 
     * @return 随机盐值
     */
    public static String generateSalt() {
        return generateSalt(DEFAULT_SALT_LENGTH);
    }
    
    /**
     * 生成指定长度的随机盐值
     * 
     * @param length 盐值长度
     * @return 随机盐值
     */
    public static String generateSalt(int length) {
        if (length <= 0) {
            length = DEFAULT_SALT_LENGTH;
        }
        
        SecureRandom random = new SecureRandom();
        StringBuilder salt = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        
        for (int i = 0; i < length; i++) {
            salt.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return salt.toString();
    }
    
    /**
     * 验证密码
     * 
     * @param plainText 明文密码
     * @param salt 盐值
     * @param encryptedText 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String plainText, String salt, String encryptedText) {
        if (StrUtil.hasBlank(plainText, encryptedText)) {
            return false;
        }
        
        String encrypted = encrypt(plainText, salt);
        return encryptedText.equals(encrypted);
    }
    
    /**
     * 验证密码（无盐值）
     * 
     * @param plainText 明文密码
     * @param encryptedText 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String plainText, String encryptedText) {
        return verify(plainText, null, encryptedText);
    }
    
    /**
     * 使用原生MessageDigest进行MD5加密
     * 
     * @param text 待加密文本
     * @return MD5加密后的字符串
     */
    public static String encryptByMessageDigest(String text) {
        if (StrUtil.isBlank(text)) {
            return null;
        }
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(text.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                result.append(String.format("%02x", b));
            }
            
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("MD5加密失败", e);
            throw new RuntimeException("MD5加密失败", e);
        }
    }
    
    /**
     * 文件MD5校验
     * 
     * @param filePath 文件路径
     * @return 文件MD5值
     */
    public static String encryptFile(String filePath) {
        if (StrUtil.isBlank(filePath)) {
            return null;
        }
        
        try {
            return DigestUtil.md5Hex(new java.io.File(filePath));
        } catch (Exception e) {
            log.error("文件MD5计算失败: {}", filePath, e);
            throw new RuntimeException("文件MD5计算失败", e);
        }
    }
    
    /**
     * 字节数组MD5加密
     * 
     * @param bytes 字节数组
     * @return MD5加密后的字符串
     */
    public static String encrypt(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return DigestUtil.md5Hex(bytes);
    }
    
    /**
     * 多次MD5加密
     * 
     * @param text 待加密文本
     * @param times 加密次数
     * @return MD5加密后的字符串
     */
    public static String encryptMultiple(String text, int times) {
        if (StrUtil.isBlank(text) || times <= 0) {
            return text;
        }
        
        String result = text;
        for (int i = 0; i < times; i++) {
            result = encrypt(result);
        }
        
        return result;
    }
    
    /**
     * 生成密码哈希（推荐用于密码存储）
     * 
     * @param password 原始密码
     * @return 包含盐值的密码哈希
     */
    public static String hashPassword(String password) {
        if (StrUtil.isBlank(password)) {
            return null;
        }
        
        String salt = generateSalt();
        String hash = encrypt(password, salt);
        
        // 返回格式：salt$hash
        return salt + "$" + hash;
    }
    
    /**
     * 验证密码哈希
     * 
     * @param password 原始密码
     * @param hashedPassword 存储的密码哈希（格式：salt$hash）
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        if (StrUtil.hasBlank(password, hashedPassword)) {
            return false;
        }
        
        if (!hashedPassword.contains("$")) {
            // 兼容无盐值的旧密码
            return verify(password, hashedPassword);
        }
        
        String[] parts = hashedPassword.split("\\$", 2);
        if (parts.length != 2) {
            return false;
        }
        
        String salt = parts[0];
        String hash = parts[1];
        
        return verify(password, salt, hash);
    }
}