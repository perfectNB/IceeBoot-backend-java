/**
 * 华为云SMS服务实现类
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.service.sms.service.impl;

import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.sms.dto.SMSRequest;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.common.service.sms.service.AbstractSMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 华为云SMS服务实现类
 * 基于华为云短信服务API实现短信发送功能
 *
 * @author CodeIcee
 * @date 2025-09-04
 */
@Slf4j
@Component
public class HuaweiSMSService extends AbstractSMSService {

    private static final String PROVIDER_NAME = SystemConstants.SMSProvider.HUAWEI;
    private static final String DEFAULT_ENDPOINT = "https://smsapi.cn-north-4.myhuaweicloud.com:443";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "华为云短信";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected Map<String, String> buildCommonParams(SMSRequest request) {
        return new HashMap<>();
    }

    @Override
    protected String generateSignature(Map<String, String> params, String accessKeySecret) throws Exception {
        // 华为云使用WSSE认证，签名在buildHeaders中完成
        return "";
    }

    @Override
    protected void addSendSmsParams(Map<String, String> params, SMSRequest request) {
        // 华为云使用JSON请求体
    }

    @Override
    protected void addBatchSmsParams(Map<String, String> params, SMSRequest request) {
        // 华为云使用JSON请求体
    }

    @Override
    protected void addQueryStatusParams(Map<String, String> params, String messageId, SMSRequest request) {
        // 华为云使用JSON请求体
    }

    // ==================== 覆盖模板方法（华为云使用JSON请求体+WSSE认证） ====================

    @Override
    public Mono<SMSResponse> sendTemplateSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("from", request.getSignName());
                requestBody.put("to", new String[]{request.getPhoneNumber()});
                requestBody.put("templateId", request.getTemplateId());

                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("templateParas", request.getTemplateParams().values().toArray(new String[0]));
                }

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildHuaweiHeaders(request, jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/batchSendSms/v1";

                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(jsonBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeout))
                        .block();

                return parseResponse(response, request.getPhoneNumber(), startTime);
            } catch (Exception e) {
                log.error("{}发送短信失败: {}", getProviderDisplayName(), e.getMessage(), e);
                return SMSResponse.failure("SEND_FAILED", "发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }

    @Override
    public Mono<SMSResponse> sendBatchSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("from", request.getSignName());
                requestBody.put("to", request.getPhoneNumbers());
                requestBody.put("templateId", request.getTemplateId());

                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("templateParas", request.getTemplateParams().values().toArray(new String[0]));
                }

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildHuaweiHeaders(request, jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/batchSendSms/v1";

                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(jsonBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeout))
                        .block();

                return parseBatchResponse(response, request.getPhoneNumbers(), startTime);
            } catch (Exception e) {
                log.error("{}批量发送失败: {}", getProviderDisplayName(), e.getMessage(), e);
                return SMSResponse.failure("BATCH_SEND_FAILED", "批量发送失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }

    @Override
    public Mono<SMSResponse> querySMSStatus(String messageId, SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/statusReport/v1?" + messageId;

                Map<String, String> headers = buildHuaweiQueryHeaders(request);

                String response = webClient.get()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeout))
                        .block();

                return parseQueryResponse(response, messageId, startTime);
            } catch (Exception e) {
                log.error("{}状态查询失败: {}", getProviderDisplayName(), e.getMessage(), e);
                return SMSResponse.failure("QUERY_FAILED", "状态查询失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }

    @Override
    public Mono<SMSResponse> queryBalance(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/balance/v1";

                Map<String, String> headers = buildHuaweiQueryHeaders(request);

                String response = webClient.get()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeout))
                        .block();

                return parseBalanceResponse(response, startTime);
            } catch (Exception e) {
                log.error("{}余额查询失败: {}", getProviderDisplayName(), e.getMessage(), e);
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
                log.error("{}配置验证失败: {}", getProviderDisplayName(), e.getMessage());
                return false;
            }
        });
    }

    // ==================== 华为云特有方法 ====================

    /**
     * 构建请求头
     */
    private Map<String, String> buildHuaweiHeaders(SMSRequest request, String payload) throws Exception {
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
    private Map<String, String> buildHuaweiQueryHeaders(SMSRequest request) throws Exception {
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

    // ==================== 响应解析 ====================

    @Override
    protected SMSResponse parseResponse(String response, String phoneNumber, long startTime) {
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

    @Override
    protected SMSResponse parseBatchResponse(String response, String[] phoneNumbers, long startTime) {
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

    @Override
    protected SMSResponse parseQueryResponse(String response, String messageId, long startTime) {
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
}