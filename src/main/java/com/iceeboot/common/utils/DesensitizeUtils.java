/**
 * 字段脱敏工具类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.iceeboot.common.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

/**
 * 字段脱敏工具类
 */
public class DesensitizeUtils {
    
    /**
     * 脱敏替换字符
     */
    private static final String MASK_CHAR = "*";
    
    /**
     * 手机号正则表达式
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    
    /**
     * 身份证号正则表达式
     */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");
    
    /**
     * 银行卡号正则表达式
     */
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^\\d{16,19}$");
    
    /**
     * 姓名脱敏
     * 规则：保留第一个字符，其余用*代替
     * 例：张三 -> 张*，张三丰 -> 张**
     * 
     * @param name 姓名
     * @return 脱敏后的姓名
     */
    public static String desensitizeName(String name) {
        if (StrUtil.isBlank(name)) {
            return name;
        }
        
        if (name.length() == 1) {
            return name;
        }
        
        return name.charAt(0) + StrUtil.repeat(MASK_CHAR, name.length() - 1);
    }
    
    /**
     * 手机号脱敏
     * 规则：保留前3位和后4位，中间用*代替
     * 例：13812345678 -> 138****5678
     * 
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizePhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        
        // 验证手机号格式
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            return phone;
        }
        
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
    
    /**
     * 邮箱脱敏
     * 规则：保留@前的前2位和@后的域名，中间用*代替
     * 例：example@gmail.com -> ex***@gmail.com
     * 
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        
        // 验证邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return email;
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email;
        }
        
        String prefix = email.substring(0, 2);
        String suffix = email.substring(atIndex);
        String mask = StrUtil.repeat(MASK_CHAR, atIndex - 2);
        
        return prefix + mask + suffix;
    }
    
    /**
     * 身份证号脱敏
     * 规则：保留前6位和后4位，中间用*代替
     * 例：110101199001011234 -> 110101********1234
     * 
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String desensitizeIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return idCard;
        }
        
        // 验证身份证号格式
        if (!ID_CARD_PATTERN.matcher(idCard).matches()) {
            return idCard;
        }
        
        return idCard.substring(0, 6) + "********" + idCard.substring(14);
    }
    
    /**
     * 银行卡号脱敏
     * 规则：保留前4位和后4位，中间用*代替
     * 例：6222021234567890 -> 6222********7890
     * 
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String desensitizeBankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return bankCard;
        }
        
        // 验证银行卡号格式
        if (!BANK_CARD_PATTERN.matcher(bankCard).matches()) {
            return bankCard;
        }
        
        if (bankCard.length() <= 8) {
            return bankCard;
        }
        
        String prefix = bankCard.substring(0, 4);
        String suffix = bankCard.substring(bankCard.length() - 4);
        String mask = StrUtil.repeat(MASK_CHAR, bankCard.length() - 8);
        
        return prefix + mask + suffix;
    }
    
    /**
     * 地址脱敏
     * 规则：保留前6个字符，其余用*代替
     * 例：北京市朝阳区某某街道123号 -> 北京市朝阳区******
     * 
     * @param address 地址
     * @return 脱敏后的地址
     */
    public static String desensitizeAddress(String address) {
        if (StrUtil.isBlank(address)) {
            return address;
        }
        
        if (address.length() <= 6) {
            return address;
        }
        
        return address.substring(0, 6) + StrUtil.repeat(MASK_CHAR, address.length() - 6);
    }
    
    /**
     * 密码脱敏
     * 规则：全部用*代替
     * 
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String desensitizePassword(String password) {
        if (StrUtil.isBlank(password)) {
            return password;
        }
        
        return StrUtil.repeat(MASK_CHAR, password.length());
    }
    
    /**
     * 车牌号脱敏
     * 规则：保留前2位和后1位，中间用*代替
     * 例：京A12345 -> 京A***5
     * 
     * @param carNumber 车牌号
     * @return 脱敏后的车牌号
     */
    public static String desensitizeCarNumber(String carNumber) {
        if (StrUtil.isBlank(carNumber)) {
            return carNumber;
        }
        
        if (carNumber.length() <= 3) {
            return carNumber;
        }
        
        String prefix = carNumber.substring(0, 2);
        String suffix = carNumber.substring(carNumber.length() - 1);
        String mask = StrUtil.repeat(MASK_CHAR, carNumber.length() - 3);
        
        return prefix + mask + suffix;
    }
    
    /**
     * 自定义脱敏
     * 
     * @param str 原始字符串
     * @param prefixKeep 前面保留位数
     * @param suffixKeep 后面保留位数
     * @return 脱敏后的字符串
     */
    public static String desensitizeCustom(String str, int prefixKeep, int suffixKeep) {
        if (StrUtil.isBlank(str)) {
            return str;
        }
        
        if (prefixKeep < 0) {
            prefixKeep = 0;
        }
        if (suffixKeep < 0) {
            suffixKeep = 0;
        }
        
        int length = str.length();
        if (prefixKeep + suffixKeep >= length) {
            return str;
        }
        
        String prefix = prefixKeep > 0 ? str.substring(0, prefixKeep) : "";
        String suffix = suffixKeep > 0 ? str.substring(length - suffixKeep) : "";
        String mask = StrUtil.repeat(MASK_CHAR, length - prefixKeep - suffixKeep);
        
        return prefix + mask + suffix;
    }
    
    /**
     * 根据类型自动脱敏
     * 
     * @param value 原始值
     * @param type 脱敏类型
     * @return 脱敏后的值
     */
    public static String desensitizeByType(String value, DesensitizeType type) {
        if (StrUtil.isBlank(value) || type == null) {
            return value;
        }
        
        return switch (type) {
            case NAME -> desensitizeName(value);
            case PHONE -> desensitizePhone(value);
            case EMAIL -> desensitizeEmail(value);
            case ID_CARD -> desensitizeIdCard(value);
            case BANK_CARD -> desensitizeBankCard(value);
            case ADDRESS -> desensitizeAddress(value);
            case PASSWORD -> desensitizePassword(value);
            case CAR_NUMBER -> desensitizeCarNumber(value);
            default -> value;
        };
    }
    
    /**
     * 脱敏类型枚举
     */
    public enum DesensitizeType {
        /**
         * 姓名
         */
        NAME,
        
        /**
         * 手机号
         */
        PHONE,
        
        /**
         * 邮箱
         */
        EMAIL,
        
        /**
         * 身份证号
         */
        ID_CARD,
        
        /**
         * 银行卡号
         */
        BANK_CARD,
        
        /**
         * 地址
         */
        ADDRESS,
        
        /**
         * 密码
         */
        PASSWORD,
        
        /**
         * 车牌号
         */
        CAR_NUMBER
    }
}