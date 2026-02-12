/**
 * SMS工具类
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.sms.utils;

import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.common.service.sms.dto.SMSRequest;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * SMS工具类
 * 提供SMS操作相关的通用工具方法
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
public class SMSUtils {
    
    /**
     * 手机号正则表达式（中国大陆）
     */
    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 国际手机号正则表达式
     */
    private static final Pattern INTERNATIONAL_PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    
    /**
     * 验证码字符集
     */
    private static final String VERIFICATION_CODE_CHARS = "0123456789";
    
    /**
     * 随机数生成器
     */
    private static final Random RANDOM = new SecureRandom();
    
    /**
     * 验证中国大陆手机号格式
     * 
     * @param phoneNumber 手机号
     * @return 是否有效
     */
    public static boolean isValidChinaPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        return CHINA_PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }
    
    /**
     * 验证国际手机号格式
     * 
     * @param phoneNumber 手机号
     * @return 是否有效
     */
    public static boolean isValidInternationalPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        String cleanPhone = phoneNumber.replaceAll("[^0-9+]", "");
        return INTERNATIONAL_PHONE_PATTERN.matcher(cleanPhone).matches();
    }
    
    /**
     * 验证手机号格式（支持国内和国际）
     * 
     * @param phoneNumber 手机号
     * @return 是否有效
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return isValidChinaPhoneNumber(phoneNumber) || isValidInternationalPhoneNumber(phoneNumber);
    }
    
    /**
     * 格式化手机号（隐藏中间4位）
     * 
     * @param phoneNumber 手机号
     * @return 格式化后的手机号
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 7) {
            return phoneNumber;
        }
        
        if (phoneNumber.length() == 11 && isValidChinaPhoneNumber(phoneNumber)) {
            // 中国大陆手机号：138****1234
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        } else {
            // 其他格式：保留前3位和后4位
            int len = phoneNumber.length();
            if (len <= 7) {
                return phoneNumber.substring(0, 3) + "****";
            } else {
                return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(len - 4);
            }
        }
    }
    
    /**
     * 生成数字验证码
     * 
     * @param length 验证码长度
     * @return 验证码
     */
    public static String generateVerificationCode(int length) {
        if (length <= 0) {
            throw new IceeBootException("验证码长度必须大于0");
        }
        
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(VERIFICATION_CODE_CHARS.charAt(RANDOM.nextInt(VERIFICATION_CODE_CHARS.length())));
        }
        return code.toString();
    }
    
    /**
     * 生成6位数字验证码
     * 
     * @return 6位验证码
     */
    public static String generateVerificationCode() {
        return generateVerificationCode(6);
    }
    
    /**
     * 生成4位数字验证码
     * 
     * @return 4位验证码
     */
    public static String generateShortVerificationCode() {
        return generateVerificationCode(4);
    }
    
    /**
     * 验证验证码格式
     * 
     * @param code 验证码
     * @param expectedLength 期望长度
     * @return 是否有效
     */
    public static boolean isValidVerificationCode(String code, int expectedLength) {
        if (code == null || code.length() != expectedLength) {
            return false;
        }
        return code.matches("\\d{" + expectedLength + "}");
    }
    
    /**
     * 验证短信内容长度
     * 
     * @param content 短信内容
     * @param maxLength 最大长度
     * @return 是否有效
     */
    public static boolean isValidSMSContent(String content, int maxLength) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        return content.length() <= maxLength;
    }
    
    /**
     * 计算短信条数（按70个字符计算）
     * 
     * @param content 短信内容
     * @return 短信条数
     */
    public static int calculateSMSCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        
        // 中文字符按2个字符计算
        int charCount = 0;
        for (char c : content.toCharArray()) {
            if (isChinese(c)) {
                charCount += 2;
            } else {
                charCount += 1;
            }
        }
        
        // 每70个字符算一条短信
        return (int) Math.ceil((double) charCount / 70);
    }
    
    /**
     * 判断字符是否为中文
     * 
     * @param c 字符
     * @return 是否为中文
     */
    private static boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }
    
    /**
     * 生成唯一的短信ID
     * 
     * @return 短信ID
     */
    public static String generateSMSId() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String randomStr = String.valueOf(RANDOM.nextInt(10000));
        return "SMS" + timestamp + String.format("%04d", Integer.parseInt(randomStr));
    }
    
    /**
     * 生成请求ID
     * 
     * @return 请求ID
     */
    public static String generateRequestId() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomStr = String.valueOf(RANDOM.nextInt(100000));
        return timestamp + String.format("%05d", Integer.parseInt(randomStr));
    }
    
    /**
     * MD5哈希
     * 
     * @param input 输入字符串
     * @return MD5哈希值
     */
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("MD5哈希计算失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * SHA1哈希
     * 
     * @param input 输入字符串
     * @return SHA1哈希值
     */
    public static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("SHA1哈希计算失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 验证SMS请求参数
     * 
     * @param request SMS请求
     * @return 验证结果
     */
    public static ValidationResult validateSMSRequest(SMSRequest request) {
        if (request == null) {
            return ValidationResult.failure("SMS请求不能为空");
        }
        
        // 验证手机号
        if (request.getPhoneNumber() == null || request.getPhoneNumber().trim().isEmpty()) {
            if (request.getPhoneNumbers() == null || request.getPhoneNumbers().length == 0) {
                return ValidationResult.failure("手机号不能为空");
            }
        } else {
            if (!isValidPhoneNumber(request.getPhoneNumber())) {
                return ValidationResult.failure("手机号格式不正确: " + request.getPhoneNumber());
            }
        }
        
        // 验证批量手机号
        if (request.getPhoneNumbers() != null && request.getPhoneNumbers().length > 0) {
            for (String phoneNumber : request.getPhoneNumbers()) {
                if (!isValidPhoneNumber(phoneNumber)) {
                    return ValidationResult.failure("批量手机号中包含格式不正确的号码: " + phoneNumber);
                }
            }
        }
        
        // 验证短信内容或模板ID
        if ((request.getContent() == null || request.getContent().trim().isEmpty()) &&
            (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty())) {
            return ValidationResult.failure("短信内容或模板ID不能同时为空");
        }
        
        // 验证短信内容长度
        if (request.getContent() != null && !request.getContent().trim().isEmpty()) {
            if (!isValidSMSContent(request.getContent(), 500)) {
                return ValidationResult.failure("短信内容过长，最大长度500字符");
            }
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证模板短信请求参数
     * 
     * @param request SMS请求
     * @return 验证结果
     */
    public static ValidationResult validateTemplateSMSRequest(SMSRequest request) {
        ValidationResult basicResult = validateSMSRequest(request);
        if (!basicResult.isValid()) {
            return basicResult;
        }
        
        // 验证模板ID
        if (request.getTemplateId() == null || request.getTemplateId().trim().isEmpty()) {
            return ValidationResult.failure("模板ID不能为空");
        }
        
        // 验证签名
        if (request.getSignName() == null || request.getSignName().trim().isEmpty()) {
            return ValidationResult.failure("短信签名不能为空");
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 清理手机号（移除非数字字符，保留+号）
     * 
     * @param phoneNumber 原始手机号
     * @return 清理后的手机号
     */
    public static String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9+]", "");
    }
    
    /**
     * 格式化时间戳为字符串
     * 
     * @param timestamp 时间戳
     * @param pattern 格式模式
     * @return 格式化后的时间字符串
     */
    public static String formatTimestamp(long timestamp, String pattern) {
        try {
            LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, java.time.ZoneOffset.UTC);
            return dateTime.format(DateTimeFormatter.ofPattern(pattern));
        } catch (Exception e) {
            log.error("时间戳格式化失败: {}", e.getMessage(), e);
            return String.valueOf(timestamp);
        }
    }
    
    /**
     * 获取当前时间戳字符串
     * 
     * @param pattern 格式模式
     * @return 时间戳字符串
     */
    public static String getCurrentTimestamp(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;
        
        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
    }
}