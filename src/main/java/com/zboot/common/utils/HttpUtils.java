/**
 * HTTP工具类
 * 
 * @author CodeIcee
 * @date 2025-08-11
 */
package com.zboot.common.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.zboot.common.exception.ZbootException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * HTTP工具类
 */
@Slf4j
@Component
public class HttpUtils {
    
    /**
     * 默认超时时间（毫秒）
     */
    private static final int DEFAULT_TIMEOUT = 30000;
    
    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @return 响应结果
     */
    public static String get(String url) {
        return get(url, null, null);
    }
    
    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @return 响应结果
     */
    public static String get(String url, Map<String, Object> params) {
        return get(url, params, null);
    }
    
    /**
     * 发送GET请求
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @param headers 请求头
     * @return 响应结果
     */
    public static String get(String url, Map<String, Object> params, Map<String, String> headers) {
        try {
            HttpRequest request = HttpUtil.createGet(url)
                    .timeout(DEFAULT_TIMEOUT);
            
            if (params != null && !params.isEmpty()) {
                request.form(params);
            }
            
            if (headers != null && !headers.isEmpty()) {
                request.headerMap(headers, true);
            }
            
            HttpResponse response = request.execute();
            log.info("GET请求: {}, 状态码: {}", url, response.getStatus());
            
            return response.body();
        } catch (Exception e) {
            log.error("GET请求失败: {}", url, e);
            throw new ZbootException("HTTP GET请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送POST请求（JSON格式）
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @return 响应结果
     */
    public static String postJson(String url, String jsonBody) {
        return postJson(url, jsonBody, null);
    }
    
    /**
     * 发送POST请求（JSON格式）
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @param headers 请求头
     * @return 响应结果
     */
    public static String postJson(String url, String jsonBody, Map<String, String> headers) {
        try {
            HttpRequest request = HttpUtil.createPost(url)
                    .timeout(DEFAULT_TIMEOUT)
                    .contentType("application/json")
                    .body(jsonBody);
            
            if (headers != null && !headers.isEmpty()) {
                request.headerMap(headers, true);
            }
            
            HttpResponse response = request.execute();
            log.info("POST JSON请求: {}, 状态码: {}", url, response.getStatus());
            
            return response.body();
        } catch (Exception e) {
            log.error("POST JSON请求失败: {}", url, e);
            throw new ZbootException("HTTP POST JSON请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送POST请求（对象转JSON）
     * 
     * @param url 请求URL
     * @param object 请求对象
     * @return 响应结果
     */
    public static String postJson(String url, Object object) {
        return postJson(url, JSONUtil.toJsonStr(object), null);
    }
    
    /**
     * 发送POST请求（对象转JSON）
     * 
     * @param url 请求URL
     * @param object 请求对象
     * @param headers 请求头
     * @return 响应结果
     */
    public static String postJson(String url, Object object, Map<String, String> headers) {
        return postJson(url, JSONUtil.toJsonStr(object), headers);
    }
    
    /**
     * 发送POST请求（表单格式）
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @return 响应结果
     */
    public static String postForm(String url, Map<String, Object> params) {
        return postForm(url, params, null);
    }
    
    /**
     * 发送POST请求（表单格式）
     * 
     * @param url 请求URL
     * @param params 请求参数
     * @param headers 请求头
     * @return 响应结果
     */
    public static String postForm(String url, Map<String, Object> params, Map<String, String> headers) {
        try {
            HttpRequest request = HttpUtil.createPost(url)
                    .timeout(DEFAULT_TIMEOUT);
            
            if (params != null && !params.isEmpty()) {
                request.form(params);
            }
            
            if (headers != null && !headers.isEmpty()) {
                request.headerMap(headers, true);
            }
            
            HttpResponse response = request.execute();
            log.info("POST FORM请求: {}, 状态码: {}", url, response.getStatus());
            
            return response.body();
        } catch (Exception e) {
            log.error("POST FORM请求失败: {}", url, e);
            throw new ZbootException("HTTP POST FORM请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送PUT请求（JSON格式）
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @return 响应结果
     */
    public static String putJson(String url, String jsonBody) {
        return putJson(url, jsonBody, null);
    }
    
    /**
     * 发送PUT请求（JSON格式）
     * 
     * @param url 请求URL
     * @param jsonBody JSON请求体
     * @param headers 请求头
     * @return 响应结果
     */
    public static String putJson(String url, String jsonBody, Map<String, String> headers) {
        try {
            HttpRequest request = HttpRequest.put(url)
                    .timeout(DEFAULT_TIMEOUT)
                    .contentType("application/json")
                    .body(jsonBody);
            
            if (headers != null && !headers.isEmpty()) {
                request.headerMap(headers, true);
            }
            
            HttpResponse response = request.execute();
            log.info("PUT JSON请求: {}, 状态码: {}", url, response.getStatus());
            
            return response.body();
        } catch (Exception e) {
            log.error("PUT JSON请求失败: {}", url, e);
            throw new ZbootException("HTTP PUT JSON请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL
     * @return 响应结果
     */
    public static String delete(String url) {
        return delete(url, null);
    }
    
    /**
     * 发送DELETE请求
     * 
     * @param url 请求URL
     * @param headers 请求头
     * @return 响应结果
     */
    public static String delete(String url, Map<String, String> headers) {
        try {
            HttpRequest request = HttpRequest.delete(url)
                    .timeout(DEFAULT_TIMEOUT);
            
            if (headers != null && !headers.isEmpty()) {
                request.headerMap(headers, true);
            }
            
            HttpResponse response = request.execute();
            log.info("DELETE请求: {}, 状态码: {}", url, response.getStatus());
            
            return response.body();
        } catch (Exception e) {
            log.error("DELETE请求失败: {}", url, e);
            throw new ZbootException("HTTP DELETE请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     * 
     * @param url 文件URL
     * @param destPath 目标路径
     */
    public static void downloadFile(String url, String destPath) {
        try {
            HttpUtil.downloadFile(url, destPath);
            log.info("文件下载成功: {} -> {}", url, destPath);
        } catch (Exception e) {
            log.error("文件下载失败: {}", url, e);
            throw new ZbootException("文件下载失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查URL是否可访问
     * 
     * @param url 请求URL
     * @return 是否可访问
     */
    public static boolean isUrlAccessible(String url) {
        try {
            HttpResponse response = HttpUtil.createGet(url)
                    .timeout(5000)
                    .execute();
            return response.getStatus() == 200;
        } catch (Exception e) {
            log.warn("URL不可访问: {}", url);
            return false;
        }
    }
}