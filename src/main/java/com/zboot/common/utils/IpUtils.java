/**
 * IP工具类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.common.utils;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import com.zboot.common.exception.ZbootException;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

/**
 * IP工具类
 */
@Slf4j
public class IpUtils {
    
    /**
     * 未知IP
     */
    private static final String UNKNOWN = "unknown";
    
    /**
     * 本地IP
     */
    private static final String LOCALHOST_IP = "127.0.0.1";
    
    /**
     * 本地IPv6
     */
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    
    /**
     * IP地址正则表达式
     */
    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    
    /**
     * 获取客户端IP地址
     * 
     * @param request HttpServletRequest
     * @return 客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                ip = ip.substring(0, index);
            }
        }
        
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        
        if (StrUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (LOCALHOST_IP.equals(ip) || LOCALHOST_IPV6.equals(ip)) {
                // 根据网卡取本机配置的IP
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ip = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    log.error("获取本机IP失败", e);
                }
            }
        }
        
        return StrUtil.isBlank(ip) ? UNKNOWN : ip.trim();
    }
    
    /**
     * 获取本机IP地址
     * 
     * @return 本机IP地址
     */
    public static String getLocalIp() {
        try {
            return NetUtil.getLocalhostStr();
        } catch (Exception e) {
            log.error("获取本机IP失败", e);
            return LOCALHOST_IP;
        }
    }
    
    /**
     * 获取本机主机名
     * 
     * @return 本机主机名
     */
    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.error("获取本机主机名失败", e);
            return "unknown";
        }
    }
    
    /**
     * 验证IP地址格式是否正确
     * 
     * @param ip IP地址
     * @return 是否为有效IP地址
     */
    public static boolean isValidIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        return IP_PATTERN.matcher(ip).matches();
    }
    
    /**
     * 判断是否为内网IP
     * 
     * @param ip IP地址
     * @return 是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (!isValidIp(ip)) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        int firstOctet = Integer.parseInt(parts[0]);
        int secondOctet = Integer.parseInt(parts[1]);
        
        // 10.0.0.0 - 10.255.255.255
        if (firstOctet == 10) {
            return true;
        }
        
        // 172.16.0.0 - 172.31.255.255
        if (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31) {
            return true;
        }
        
        // 192.168.0.0 - 192.168.255.255
        if (firstOctet == 192 && secondOctet == 168) {
            return true;
        }
        
        // 127.0.0.0 - 127.255.255.255 (本地回环)
        if (firstOctet == 127) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 将IP地址转换为长整型
     * 
     * @param ip IP地址
     * @return 长整型IP
     */
    public static long ipToLong(String ip) {
        if (!isValidIp(ip)) {
            throw new ZbootException("无效的IP地址: " + ip);
        }
        
        String[] parts = ip.split("\\.");
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result * 256 + Integer.parseInt(parts[i]);
        }
        return result;
    }
    
    /**
     * 将长整型转换为IP地址
     * 
     * @param longIp 长整型IP
     * @return IP地址
     */
    public static String longToIp(long longIp) {
        return ((longIp >> 24) & 0xFF) + "." +
               ((longIp >> 16) & 0xFF) + "." +
               ((longIp >> 8) & 0xFF) + "." +
               (longIp & 0xFF);
    }
    
    /**
     * 获取IP地址的地理位置信息
     * TODO: 可以集成第三方IP地址库实现
     * 
     * @param ip IP地址
     * @return 地理位置信息
     */
    public static String getIpLocation(String ip) {
        if (!isValidIp(ip)) {
            return "未知";
        }
        
        if (isInternalIp(ip)) {
            return "内网IP";
        }
        
        // TODO: 集成第三方IP地址库，如：ip2region、GeoLite2等
        return "未知";
    }
    
    /**
     * 脱敏IP地址（隐藏最后一段）
     * 
     * @param ip IP地址
     * @return 脱敏后的IP地址
     */
    public static String desensitizeIp(String ip) {
        if (!isValidIp(ip)) {
            return ip;
        }
        
        String[] parts = ip.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
    }
}