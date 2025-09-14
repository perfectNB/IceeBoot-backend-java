/**
 * 容联云SMS服务实现类
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 容联云SMS服务实现类
 * 基于容联云短信服务API实现短信发送功能
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public class RongLianSMSService implements SMSService {
    
    private static final String PROVIDER_NAME = "ronglian";
    private static final String DEFAULT_ENDPOINT = "https://app.cloopen.com:8883";
    private static final String API_VERSION = "2013-12-26";
    private static final Integer DEFAULT_TIMEOUT = 30;
    
    private final WebClient webClient;
    
    public RongLianSMSService() {
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
                requestBody.put("to", request.getPhoneNumber());
                requestBody.put("templateId", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("datas", request.getTemplateParams().values().toArray(new String[0]));
                }
                
                String jsonBody = convertToJson(requestBody);
                
                // 构建请求头
                Map<String, String> headers = buildHeaders(request);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = String.format("%s/%s/Accounts/%s/SMS/TemplateSMS?sig=%s", 
                        endpoint, API_VERSION, request.getAccessKeyId(), generateSig(request));
                
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
                log.error("容联云SMS发送失败: {}", e.getMessage(), e);
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
                // 容联云批量发送需要逐个发送
                List<String> messageIds = new ArrayList<>();
                boolean allSuccess = true;
                String lastError = null;
                
                for (String phoneNumber : request.getPhoneNumbers()) {
                    SMSRequest singleRequest = new SMSRequest()
                            .setPhoneNumber(phoneNumber)
                            .setTemplateId(request.getTemplateId())
                            .setTemplateParams(request.getTemplateParams())
                            .setAccessKeyId(request.getAccessKeyId())
                            .setAccessKeySecret(request.getAccessKeySecret())
                            .setAppId(request.getAppId())
                            .setEndpoint(request.getEndpoint())
                            .setTimeout(request.getTimeout());
                    
                    SMSResponse singleResponse = sendTemplateSMS(singleRequest).block();
                    if (singleResponse.getSuccess()) {
                        messageIds.add(singleResponse.getMessageId());
                    } else {
                        allSuccess = false;
                        lastError = singleResponse.getErrorMessage();
                    }
                }
                
                if (allSuccess) {
                    return SMSResponse.success()
                            .setProvider(getProvider())
                            .setMessageIds(messageIds.toArray(new String[0]))
                            .setPhoneNumbers(request.getPhoneNumbers())
                            .setStatus("SENT")
                            .setResponseTime(System.currentTimeMillis() - startTime);
                } else {
                    return SMSResponse.failure("BATCH_PARTIAL_FAILED", "批量发送部分失败: " + lastError)
                            .setProvider(getProvider())
                            .setPhoneNumbers(request.getPhoneNumbers())
                            .setResponseTime(System.currentTimeMillis() - startTime);
                }
                
            } catch (Exception e) {
                log.error("容联云SMS批量发送失败: {}", e.getMessage(), e);
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
                // 构建请求头
                Map<String, String> headers = buildHeaders(request);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = String.format("%s/%s/Accounts/%s/SMS/QuerySMSStatus?sig=%s&smsMessageSid=%s", 
                        endpoint, API_VERSION, request.getAccessKeyId(), generateSig(request), messageId);
                
                String response = webClient.get()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseQueryResponse(response, messageId, startTime);
                
            } catch (Exception e) {
                log.error("容联云SMS状态查询失败: {}", e.getMessage(), e);
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
                // 构建请求头
                Map<String, String> headers = buildHeaders(request);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String url = String.format("%s/%s/Accounts/%s/AccountInfo?sig=%s", 
                        endpoint, API_VERSION, request.getAccessKeyId(), generateSig(request));
                
                String response = webClient.get()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseBalanceResponse(response, startTime);
                
            } catch (Exception e) {
                log.error("容联云SMS余额查询失败: {}", e.getMessage(), e);
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
                log.error("容联云SMS配置验证失败: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 构建请求头
     */
    private Map<String, String> buildHeaders(SMSRequest request) throws Exception {
        Map<String, String> headers = new HashMap<>();
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String authorization = Base64.getEncoder().encodeToString(
                (request.getAccessKeyId() + ":" + timestamp).getBytes(StandardCharsets.UTF_8)
        );
        
        headers.put("Content-Type", "application/json;charset=utf-8");
        headers.put("Accept", "application/json");
        headers.put("Authorization", authorization);
        
        return headers;
    }
    
    /**
     * 生成签名
     */
    private String generateSig(SMSRequest request) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String sigStr = request.getAccessKeyId() + request.getAccessKeySecret() + timestamp;
        return md5(sigStr).toUpperCase();
    }
    
    /**
     * MD5哈希
     */
    private String md5(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
            if (response.contains("\"statusCode\":\"000000\"")) {
                String messageId = extractJsonValue(response, "smsMessageSid");
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "statusCode");
                String errorMessage = extractJsonValue(response, "statusMsg");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumber(phoneNumber)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析容联云SMS响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setPhoneNumber(phoneNumber)
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
            if (response.contains("\"status\":\"SUCCESS\"")) {
                status = "SUCCESS";
            } else if (response.contains("\"status\":\"FAILED\"")) {
                status = "FAILED";
            } else if (response.contains("\"status\":\"SENDING\"")) {
                status = "SENDING";
            }
            
            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus(status)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        } catch (Exception e) {
            log.error("解析容联云SMS查询响应失败: {}", e.getMessage(), e);
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
            log.error("解析容联云SMS余额响应失败: {}", e.getMessage(), e);
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