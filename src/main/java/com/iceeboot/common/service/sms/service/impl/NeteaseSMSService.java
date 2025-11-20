/**
 * 网易云信SMS服务实现类
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.iceeboot.common.service.sms.service.impl;

import com.iceeboot.common.service.sms.dto.SMSRequest;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.common.service.sms.service.SMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 网易云信SMS服务实现类
 * 基于网易云信短信服务API实现短信发送功能
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public class NeteaseSMSService implements SMSService {
    
    private static final String PROVIDER_NAME = "netease";
    private static final String DEFAULT_ENDPOINT = "https://api.netease.im";
    private static final Integer DEFAULT_TIMEOUT = 30;
    
    private final WebClient webClient;
    
    public NeteaseSMSService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }
    
    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Mono<SMSResponse> sendSMS(SMSRequest request) {
        return sendTemplateSMS(request);
    }
    
    @Override
    public Mono<SMSResponse> sendTemplateSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 构建请求参数
                Map<String, String> params = new HashMap<>();
                params.put("mobile", request.getPhoneNumber());
                params.put("templateid", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    // 网易云信模板参数格式：["param1", "param2"]
                    StringBuilder paramsJson = new StringBuilder("[");
                    boolean first = true;
                    for (String value : request.getTemplateParams().values()) {
                        if (!first) paramsJson.append(",");
                        paramsJson.append("\"").append(value).append("\"");
                        first = false;
                    }
                    paramsJson.append("]");
                    params.put("params", paramsJson.toString());
                }
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request, params);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/sendtemplate.action";
                
                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseResponse(response, request.getPhoneNumber(), startTime);
                
            } catch (Exception e) {
                log.error("网易云信SMS发送失败: {}", e.getMessage(), e);
                return SMSResponse.failure("SEND_FAILED", "发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setPhoneNumber(request.getPhoneNumber())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<SMSResponse> sendBatchSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 构建请求参数
                Map<String, String> params = new HashMap<>();
                params.put("mobiles", String.join(",", request.getPhoneNumbers()));
                params.put("templateid", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    StringBuilder paramsJson = new StringBuilder("[");
                    boolean first = true;
                    for (String value : request.getTemplateParams().values()) {
                        if (!first) paramsJson.append(",");
                        paramsJson.append("\"").append(value).append("\"");
                        first = false;
                    }
                    paramsJson.append("]");
                    params.put("params", paramsJson.toString());
                }
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request, params);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/sendtemplate.action";
                
                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseBatchResponse(response, request.getPhoneNumbers(), startTime);
                
            } catch (Exception e) {
                log.error("网易云信SMS批量发送失败: {}", e.getMessage(), e);
                return SMSResponse.failure("BATCH_SEND_FAILED", "批量发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setPhoneNumbers(request.getPhoneNumbers())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<SMSResponse> sendVerificationCode(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 网易云信验证码短信
                Map<String, String> params = new HashMap<>();
                params.put("mobile", request.getPhoneNumber());
                params.put("authCode", request.getTemplateParams().get("code"));
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request, params);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/sendcode.action";
                
                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseResponse(response, request.getPhoneNumber(), startTime);
                
            } catch (Exception e) {
                log.error("网易云信SMS验证码发送失败: {}", e.getMessage(), e);
                return SMSResponse.failure("SEND_VERIFICATION_FAILED", "验证码发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setPhoneNumber(request.getPhoneNumber())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<SMSResponse> sendNotification(SMSRequest request) {
        return sendTemplateSMS(request.setSmsType("notification"));
    }
    
    @Override
    public Mono<SMSResponse> sendMarketing(SMSRequest request) {
        return sendTemplateSMS(request.setSmsType("marketing"));
    }
    
    @Override
    public Mono<SMSResponse> querySMSStatus(String messageId, SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 构建请求参数
                Map<String, String> params = new HashMap<>();
                params.put("sendid", messageId);
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request, params);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/querystatus.action";
                
                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseQueryResponse(response, messageId, startTime);
                
            } catch (Exception e) {
                log.error("网易云信SMS状态查询失败: {}", e.getMessage(), e);
                return SMSResponse.failure("QUERY_FAILED", "状态查询失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<SMSResponse> queryBalance(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                // 网易云信不直接提供余额查询API，返回不支持
                return SMSResponse.failure("NOT_SUPPORTED", "网易云信SMS不支持余额查询")
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                
            } catch (Exception e) {
                log.error("网易云信SMS余额查询失败: {}", e.getMessage(), e);
                return SMSResponse.failure("QUERY_BALANCE_FAILED", "余额查询失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<Boolean> validateConfig(SMSRequest request) {
        return Mono.fromCallable(() -> {
            try {
                if (request.getAppKey() == null || request.getAppKey().trim().isEmpty()) {
                    return false;
                }
                if (request.getAccessKeySecret() == null || request.getAccessKeySecret().trim().isEmpty()) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error("网易云信SMS配置验证失败: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 构建请求头
     */
    private Map<String, String> buildHeaders(SMSRequest request, Map<String, String> params) throws Exception {
        Map<String, String> headers = new HashMap<>();
        
        String appKey = request.getAppKey();
        String appSecret = request.getAccessKeySecret();
        String nonce = UUID.randomUUID().toString();
        String curTime = String.valueOf(System.currentTimeMillis() / 1000);
        
        // 计算CheckSum
        String checkSum = sha1(appSecret + nonce + curTime);
        
        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.put("AppKey", appKey);
        headers.put("Nonce", nonce);
        headers.put("CurTime", curTime);
        headers.put("CheckSum", checkSum);
        
        return headers;
    }
    
    /**
     * SHA1哈希
     */
    private String sha1(String input) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * 构建表单数据
     */
    private String buildFormData(Map<String, String> params) {
        StringBuilder formData = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                formData.append("&");
            }
            try {
                formData.append(java.net.URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.name()))
                        .append("=")
                        .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                formData.append(entry.getKey()).append("=").append(entry.getValue());
            }
            first = false;
        }
        return formData.toString();
    }
    
    /**
     * 解析单条短信响应
     */
    private SMSResponse parseResponse(String response, String phoneNumber, long startTime) {
        try {
            if (response.contains("\"code\":200")) {
                String messageId = extractJsonValue(response, "sendid");
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "code");
                String errorMessage = extractJsonValue(response, "desc");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumber(phoneNumber)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析网易云信SMS响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setPhoneNumber(phoneNumber)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        }
    }
    
    /**
     * 解析批量短信响应
     */
    private SMSResponse parseBatchResponse(String response, String[] phoneNumbers, long startTime) {
        try {
            if (response.contains("\"code\":200")) {
                String messageId = extractJsonValue(response, "sendid");
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setPhoneNumbers(phoneNumbers)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "code");
                String errorMessage = extractJsonValue(response, "desc");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumbers(phoneNumbers)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析网易云信SMS批量响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setPhoneNumbers(phoneNumbers)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        }
    }
    
    /**
     * 解析查询响应
     */
    private SMSResponse parseQueryResponse(String response, String messageId, long startTime) {
        try {
            String status = "UNKNOWN";
            if (response.contains("\"status\":1")) {
                status = "SUCCESS";
            } else if (response.contains("\"status\":2")) {
                status = "FAILED";
            } else if (response.contains("\"status\":0")) {
                status = "WAITING";
            }
            
            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus(status)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        } catch (Exception e) {
            log.error("解析网易云信SMS查询响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        }
    }
    
    /**
     * 从JSON字符串中提取值
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\":\"([^\"]*)\";";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
            
            // 尝试数字值
            pattern = "\"" + key + "\":([^,}]*)";
            p = java.util.regex.Pattern.compile(pattern);
            m = p.matcher(json);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            log.warn("提取JSON值失败: key={}, error={}", key, e.getMessage());
        }
        return null;
    }
}