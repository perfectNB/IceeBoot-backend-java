/**
 * 阿里云SMS服务实现类
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * 阿里云SMS服务实现类
 * 基于阿里云短信服务API实现短信发送功能
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public class AliyunSMSService implements SMSService {
    
    private static final String PROVIDER_NAME = "aliyun";
    private static final String DEFAULT_ENDPOINT = "https://dysmsapi.aliyuncs.com";
    private static final String API_VERSION = "2017-05-25";
    private static final Integer DEFAULT_TIMEOUT = 30;
    
    private final WebClient webClient;
    
    public AliyunSMSService() {
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
                Map<String, String> params = buildCommonParams(request);
                params.put("Action", "SendSms");
                params.put("PhoneNumbers", request.getPhoneNumber());
                params.put("SignName", request.getSignName());
                params.put("TemplateCode", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    params.put("TemplateParam", convertMapToJson(request.getTemplateParams()));
                }
                
                // 生成签名
                String signature = generateSignature(params, request.getAccessKeySecret());
                params.put("Signature", signature);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String response = webClient.post()
                        .uri(endpoint)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .bodyValue(buildQueryString(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseResponse(response, request.getPhoneNumber(), startTime);
                
            } catch (Exception e) {
                log.error("阿里云SMS发送失败: {}", e.getMessage(), e);
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
                Map<String, String> params = buildCommonParams(request);
                params.put("Action", "SendBatchSms");
                params.put("PhoneNumberJson", convertArrayToJson(request.getPhoneNumbers()));
                params.put("SignNameJson", convertArrayToJson(new String[]{request.getSignName()}));
                params.put("TemplateCode", request.getTemplateId());
                
                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    params.put("TemplateParamJson", convertMapToJson(request.getTemplateParams()));
                }
                
                // 生成签名
                String signature = generateSignature(params, request.getAccessKeySecret());
                params.put("Signature", signature);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String response = webClient.post()
                        .uri(endpoint)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .bodyValue(buildQueryString(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseBatchResponse(response, request.getPhoneNumbers(), startTime);
                
            } catch (Exception e) {
                log.error("阿里云SMS批量发送失败: {}", e.getMessage(), e);
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
                // 构建请求参数
                Map<String, String> params = buildCommonParams(request);
                params.put("Action", "QuerySendDetails");
                params.put("PhoneNumber", request.getPhoneNumber());
                params.put("BizId", messageId);
                params.put("SendDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
                params.put("PageSize", "10");
                params.put("CurrentPage", "1");
                
                // 生成签名
                String signature = generateSignature(params, request.getAccessKeySecret());
                params.put("Signature", signature);
                
                // 发送请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : DEFAULT_ENDPOINT;
                String response = webClient.post()
                        .uri(endpoint)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .bodyValue(buildQueryString(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(request.getTimeout() != null ? request.getTimeout() : DEFAULT_TIMEOUT))
                        .block();
                
                return parseQueryResponse(response, messageId, startTime);
                
            } catch (Exception e) {
                log.error("阿里云SMS状态查询失败: {}", e.getMessage(), e);
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
                // 阿里云SMS没有直接的余额查询API，返回不支持
                return SMSResponse.failure("NOT_SUPPORTED", "阿里云SMS不支持余额查询")
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
                
            } catch (Exception e) {
                log.error("阿里云SMS余额查询失败: {}", e.getMessage(), e);
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
                return true;
            } catch (Exception e) {
                log.error("阿里云SMS配置验证失败: {}", e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 构建通用请求参数
     */
    private Map<String, String> buildCommonParams(SMSRequest request) {
        Map<String, String> params = new TreeMap<>();
        params.put("AccessKeyId", request.getAccessKeyId());
        params.put("Format", "JSON");
        params.put("RegionId", request.getRegion() != null ? request.getRegion() : "cn-hangzhou");
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureNonce", UUID.randomUUID().toString());
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")));
        params.put("Version", API_VERSION);
        return params;
    }
    
    /**
     * 生成签名
     */
    private String generateSignature(Map<String, String> params, String accessKeySecret) throws Exception {
        // 构建待签名字符串
        StringBuilder stringToSign = new StringBuilder();
        stringToSign.append("POST&%2F&");
        
        StringBuilder canonicalizedQueryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!"Signature".equals(entry.getKey())) {
                if (canonicalizedQueryString.length() > 0) {
                    canonicalizedQueryString.append("%26");
                }
                canonicalizedQueryString.append(percentEncode(entry.getKey()))
                        .append("%3D")
                        .append(percentEncode(entry.getValue()));
            }
        }
        
        stringToSign.append(percentEncode(canonicalizedQueryString.toString()));
        
        // HMAC-SHA1签名
        String key = accessKeySecret + "&";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
        
        return Base64.getEncoder().encodeToString(signData);
    }
    
    /**
     * URL编码
     */
    private String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }
    
    /**
     * 构建查询字符串
     */
    private String buildQueryString(Map<String, String> params) {
        StringBuilder queryString = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (queryString.length() > 0) {
                queryString.append("&");
            }
            queryString.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return queryString.toString();
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private String convertMapToJson(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * 将数组转换为JSON字符串
     */
    private String convertArrayToJson(String[] array) {
        if (array == null || array.length == 0) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(array[i]).append("\"");
        }
        json.append("]");
        return json.toString();
    }
    
    /**
     * 解析单条短信响应
     */
    private SMSResponse parseResponse(String response, String phoneNumber, long startTime) {
        try {
            // 简单的JSON解析（实际项目中建议使用Jackson或Gson）
            if (response.contains("\"Code\":\"OK\"")) {
                String messageId = extractJsonValue(response, "BizId");
                String requestId = extractJsonValue(response, "RequestId");
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setRequestId(requestId)
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "Code");
                String errorMessage = extractJsonValue(response, "Message");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumber(phoneNumber)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析阿里云SMS响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"Code\":\"OK\"")) {
                String requestId = extractJsonValue(response, "RequestId");
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setRequestId(requestId)
                        .setPhoneNumbers(phoneNumbers)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "Code");
                String errorMessage = extractJsonValue(response, "Message");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumbers(phoneNumbers)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析阿里云SMS批量响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"Code\":\"OK\"")) {
                String status = "UNKNOWN";
                if (response.contains("\"SendStatus\":1")) {
                    status = "WAITING";
                } else if (response.contains("\"SendStatus\":2")) {
                    status = "FAILED";
                } else if (response.contains("\"SendStatus\":3")) {
                    status = "SUCCESS";
                }
                
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setStatus(status)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                String errorCode = extractJsonValue(response, "Code");
                String errorMessage = extractJsonValue(response, "Message");
                
                return SMSResponse.failure(errorCode, errorMessage)
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析阿里云SMS查询响应失败: {}", e.getMessage(), e);
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