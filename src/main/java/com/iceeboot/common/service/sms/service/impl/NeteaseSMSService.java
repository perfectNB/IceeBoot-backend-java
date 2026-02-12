/**
 * 网易云信SMS服务实现类
 *
 * @author CodeIcee
 * @date 2025-01-24
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
@Component
public class NeteaseSMSService extends AbstractSMSService {

    private static final String PROVIDER_NAME = SystemConstants.SMSProvider.NETEASE;
    private static final String DEFAULT_ENDPOINT = "https://api.netease.im";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "网易云信短信";
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
        // 网易云信使用CheckSum认证，签名在buildNeteaseHeaders中完成
        return "";
    }

    @Override
    protected void addSendSmsParams(Map<String, String> params, SMSRequest request) {
        // 网易云信使用表单参数+自定义Header
    }

    @Override
    protected void addBatchSmsParams(Map<String, String> params, SMSRequest request) {
        // 网易云信使用表单参数+自定义Header
    }

    @Override
    protected void addQueryStatusParams(Map<String, String> params, String messageId, SMSRequest request) {
        // 网易云信使用表单参数+自定义Header
    }

    // ==================== 覆盖模板方法（网易云信使用表单参数+CheckSum认证） ====================

    @Override
    public Mono<SMSResponse> sendTemplateSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", request.getPhoneNumber());
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

                Map<String, String> headers = buildNeteaseHeaders(request);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/sendtemplate.action";

                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
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

                Map<String, String> headers = buildNeteaseHeaders(request);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/sendtemplate.action";

                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
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
    public Mono<SMSResponse> sendVerificationCode(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, String> params = new HashMap<>();
                params.put("mobile", request.getPhoneNumber());
                params.put("authCode", request.getTemplateParams().get("code"));

                Map<String, String> headers = buildNeteaseHeaders(request);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/sendcode.action";

                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeout))
                        .block();

                return parseResponse(response, request.getPhoneNumber(), startTime);
            } catch (Exception e) {
                log.error("{}验证码发送失败: {}", getProviderDisplayName(), e.getMessage(), e);
                return SMSResponse.failure("SEND_VERIFICATION_FAILED", "验证码发送失败: " + e.getMessage())
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
                Map<String, String> params = new HashMap<>();
                params.put("sendid", messageId);

                Map<String, String> headers = buildNeteaseHeaders(request);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/sms/querystatus.action";

                String response = webClient.post()
                        .uri(url)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(buildFormData(params))
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
                log.error("{}配置验证失败: {}", getProviderDisplayName(), e.getMessage());
                return false;
            }
        });
    }

    // ==================== 网易云信特有方法 ====================

    /**
     * 构建网易云信请求头
     */
    private Map<String, String> buildNeteaseHeaders(SMSRequest request) throws Exception {
        Map<String, String> headers = new HashMap<>();

        String appKey = request.getAppKey();
        String appSecret = request.getAccessKeySecret();
        String nonce = UUID.randomUUID().toString();
        String curTime = String.valueOf(System.currentTimeMillis() / 1000);

        String checkSum = sha1(appSecret + nonce + curTime);

        headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        headers.put("AppKey", appKey);
        headers.put("Nonce", nonce);
        headers.put("CurTime", curTime);
        headers.put("CheckSum", checkSum);

        return headers;
    }

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

    // ==================== 响应解析 ====================

    @Override
    protected SMSResponse parseResponse(String response, String phoneNumber, long startTime) {
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

    @Override
    protected SMSResponse parseBatchResponse(String response, String[] phoneNumbers, long startTime) {
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

    @Override
    protected SMSResponse parseQueryResponse(String response, String messageId, long startTime) {
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
}