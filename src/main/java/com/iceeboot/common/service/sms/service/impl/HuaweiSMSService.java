/**
 * 华为云SMS服务实现类
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
import java.util.*;

/**
 * 华为云SMS服务实现类
 * 基于华为云短信服务API实现短信发送功能
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public class HuaweiSMSService implements SMSService {
    
    private static final String PROVIDER_NAME = "huawei";
    private static final String DEFAULT_ENDPOINT = "https://smsapi.cn-north-4.myhuaweicloud.com:443";
    private static final String API_VERSION = "v1";
    private static final Integer DEFAULT_TIMEOUT = 30;
    
    private final WebClient webClient;
    
    public HuaweiSMSService() {
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
                // 构建请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("from", request.getSignName());
                requestBody.put("to", new String[]{request.getPhoneNumber()});
                requestBody.put("templateId", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("templateParas", request.getTemplateParams().values().toArray(new String[0]));
                }
                
                String jsonBody = convertToJson(requestBody);
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request, jsonBody);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/batchSendSms/v1";
                
                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(jsonBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseResponse(response, request.getPhoneNumber(), startTime);
                
            } catch (Exception e) {
                log.error("华为云SMS发送失败: {}", e.getMessage(), e);
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
                // 构建请求体
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("from", request.getSignName());
                requestBody.put("to", request.getPhoneNumbers());
                requestBody.put("templateId", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("templateParas", request.getTemplateParams().values().toArray(new String[0]));
                }
                
                String jsonBody = convertToJson(requestBody);
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request, jsonBody);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/batchSendSms/v1";
                
                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(jsonBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseBatchResponse(response, request.getPhoneNumbers(), startTime);
                
            } catch (Exception e) {
                log.error("华为云SMS批量发送失败: {}", e.getMessage(), e);
                return SMSResponse.failure("BATCH_SEND_FAILED", "批量发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setPhoneNumbers(request.getPhoneNumbers())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }
    
    @Override
    public Mono<SMSResponse> sendVerificationCode(SMSRequest request) {
        return sendTemplateSMS(request.setSmsType("verification"));
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
                // 华为云SMS状态查询
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/statusReport/v1?" + messageId;
                
                Map<String, String> headers = buildQueryHeaders(request);
                
                String response = webClient.get()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseQueryResponse(response, messageId, startTime);
                
            } catch (Exception e) {
                log.error("华为云SMS状态查询失败: {}", e.getMessage(), e);
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
                // 华为云SMS余额查询
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = endpoint + "/sms/balance/v1";
                
                Map<String, String> headers = buildQueryHeaders(request);
                
                String response = webClient.get()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseBalanceResponse(response, startTime);
                
            } catch (Exception e) {
                log.error("华为云SMS余额查询失败: {}", e.getMessage(), e);
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
                if (request.getAccessKeyId() == null || request.getAccessKeyId().trim().isEmpty()) {
                    return false;
                }
                if (request.getAccessKeySecret() == null || request.getAccessKeySecret().trim().isEmpty()) {
                    return false;
                }
                if (request.getAppId() == null || request.getAppId().trim().isEmpty()) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.error("华为云SMS配置验证失败: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 构建请求头
     */
    private Map<String, String> buildHeaders(SMSRequest request, String payload) throws Exception {
        Map<String, String> headers = new HashMap<>();
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("Authorization", "WSSE realm=\"SDP\",profile=\"UsernameToken\",type=\"Appkey\"");
        headers.put("X-WSSE", buildWSSEHeader(request.getAccessKeyId(), request.getAccessKeySecret(), timestamp, nonce));
        
        return headers;
    }
    
    /**
     * 构建查询请求头
     */
    private Map<String, String> buildQueryHeaders(SMSRequest request) throws Exception {
        Map<String, String> headers = new HashMap<>();
        
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        
        headers.put("Authorization", "WSSE realm=\"SDP\",profile=\"UsernameToken\",type=\"Appkey\"");
        headers.put("X-WSSE", buildWSSEHeader(request.getAccessKeyId(), request.getAccessKeySecret(), timestamp, nonce));
        
        return headers;
    }
    
    /**
     * 构建WSSE认证头
     */
    private String buildWSSEHeader(String appKey, String appSecret, String timestamp, String nonce) throws Exception {
        // 构建PasswordDigest
        String passwordDigest = Base64.getEncoder().encodeToString(
                sha256((nonce + timestamp + appSecret).getBytes(StandardCharsets.UTF_8))
        );
        
        return String.format(
                "UsernameToken Username=\"%s\",PasswordDigest=\"%s\",Nonce=\"%s\",Created=\"%s\"",
                appKey,
                passwordDigest,
                Base64.getEncoder().encodeToString(nonce.getBytes(StandardCharsets.UTF_8)),
                timestamp
        );
    }
    
    /**
     * SHA256哈希
     */
    private byte[] sha256(byte[] data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(data);
    }
    
    /**
     * 将对象转换为JSON字符串
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":");
            
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof String[]) {
                json.append("[");
                String[] array = (String[]) value;
                for (int i = 0; i < array.length; i++) {
                    if (i > 0) json.append(",");
                    json.append("\"").append(array[i]).append("\"");
                }
                json.append("]");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * 解析单条短信响应
     */
    private SMSResponse parseResponse(String response, String phoneNumber, long startTime) {
        try {
            if (response.contains("\"code\":\"000000\"")) {
                String messageId = extractJsonValue(response, "smsMsgId");
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "code");
                String errorMessage = extractJsonValue(response, "description");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumber(phoneNumber)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析华为云SMS响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"code\":\"000000\"")) {
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setPhoneNumbers(phoneNumbers)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "code");
                String errorMessage = extractJsonValue(response, "description");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumbers(phoneNumbers)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析华为云SMS批量响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"status\":\"DELIVRD\"")) {
                status = "SUCCESS";
            } else if (response.contains("\"status\":\"FAILED\"")) {
                status = "FAILED";
            }
            
            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus(status)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        } catch (Exception e) {
            log.error("解析华为云SMS查询响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        }
    }
    
    /**
     * 解析余额响应
     */
    private SMSResponse parseBalanceResponse(String response, long startTime) {
        try {
            String balance = extractJsonValue(response, "balance");
            
            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setStatus("SUCCESS")
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response)
                    .setExtraData(Map.of("balance", balance != null ? balance : "未知"));
        } catch (Exception e) {
            log.error("解析华为云SMS余额响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
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