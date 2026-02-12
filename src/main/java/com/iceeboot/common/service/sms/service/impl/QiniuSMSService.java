/**
 * 七牛云SMS服务实现类
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.iceeboot.common.service.sms.service.impl;

import com.iceeboot.common.service.sms.dto.SMSRequest;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import com.iceeboot.common.service.sms.service.AbstractSMSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 七牛云SMS服务实现类
 * 基于七牛云短信服务API实现短信发送功能
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
@Component
public class QiniuSMSService extends AbstractSMSService {

    private static final String PROVIDER_NAME = "qiniu";
    private static final String DEFAULT_ENDPOINT = "https://sms.qiniuapi.com";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "七牛云短信";
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
        // 七牛云签名在buildQiniuHeaders中完成
        return "";
    }

    @Override
    protected void addSendSmsParams(Map<String, String> params, SMSRequest request) {
        // 七牛云使用JSON请求体
    }

    @Override
    protected void addBatchSmsParams(Map<String, String> params, SMSRequest request) {
        // 七牛云使用JSON请求体
    }

    @Override
    protected void addQueryStatusParams(Map<String, String> params, String messageId, SMSRequest request) {
        // 七牛云使用JSON请求体
    }

    // ==================== 覆盖模板方法（七牛云使用JSON请求体+自定义认证） ====================

    @Override
    public Mono<SMSResponse> sendTemplateSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("template_id", request.getTemplateId());
                requestBody.put("mobiles", new String[]{request.getPhoneNumber()});

                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    Map<String, String> parameters = new HashMap<>();
                    int index = 1;
                    for (String value : request.getTemplateParams().values()) {
                        parameters.put(String.valueOf(index++), value);
                    }
                    requestBody.put("parameters", parameters);
                }

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildQiniuHeaders(request, "POST", "/v1/message", jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/v1/message";

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
                requestBody.put("template_id", request.getTemplateId());
                requestBody.put("mobiles", request.getPhoneNumbers());

                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    Map<String, String> parameters = new HashMap<>();
                    int index = 1;
                    for (String value : request.getTemplateParams().values()) {
                        parameters.put(String.valueOf(index++), value);
                    }
                    requestBody.put("parameters", parameters);
                }

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildQiniuHeaders(request, "POST", "/v1/message", jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/v1/message";

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
                Map<String, String> headers = buildQiniuHeaders(request, "GET", "/v1/message/" + messageId, "");

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();
                String url = endpoint + "/v1/message/" + messageId;

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

    // ==================== 七牛云特有方法 ====================

    /**
     * 构建七牛云请求头
     */
    private Map<String, String> buildQiniuHeaders(SMSRequest request, String method, String path, String body) throws Exception {
        Map<String, String> headers = new HashMap<>();

        String contentType = "application/json";

        headers.put("Content-Type", contentType);
        headers.put("Authorization", "Qiniu " + generateQiniuToken(request, method, path, body, contentType));

        return headers;
    }

    /**
     * 生成七牛云认证Token
     */
    private String generateQiniuToken(SMSRequest request, String method, String path, String body, String contentType) throws Exception {
        StringBuilder signingStr = new StringBuilder();
        signingStr.append(method).append(" ").append(path).append("\n");
        signingStr.append("Host: sms.qiniuapi.com\n");
        signingStr.append("Content-Type: ").append(contentType).append("\n");
        signingStr.append("\n");

        if (body != null && !body.isEmpty()) {
            signingStr.append(body);
        }

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKeySpec = new SecretKeySpec(request.getAccessKeySecret().getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(secretKeySpec);
        byte[] digest = mac.doFinal(signingStr.toString().getBytes(StandardCharsets.UTF_8));

        String encodedSign = Base64.getEncoder().encodeToString(digest);
        String accessToken = request.getAccessKeyId() + ":" + encodedSign;

        return Base64.getEncoder().encodeToString(accessToken.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将对象转换为JSON字符串
     */
    @SuppressWarnings("unchecked")
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
            } else if (value instanceof Map) {
                json.append(convertMapToJson((Map<String, String>) value));
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
            if (response.contains("\"job_id\"")) {
                String messageId = extractJsonValue(response, "job_id");

                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else if (response.contains("\"error\"")) {
                String errorMessage = extractJsonValue(response, "error");

                return SMSResponse.failure("API_ERROR", errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumber(phoneNumber)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析七牛云SMS响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"job_id\"")) {
                String messageId = extractJsonValue(response, "job_id");

                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(messageId)
                        .setPhoneNumbers(phoneNumbers)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else if (response.contains("\"error\"")) {
                String errorMessage = extractJsonValue(response, "error");

                return SMSResponse.failure("API_ERROR", errorMessage)
                        .setProvider(getProvider())
                        .setPhoneNumbers(phoneNumbers)
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else {
                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setPhoneNumbers(phoneNumbers)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            }
        } catch (Exception e) {
            log.error("解析七牛云SMS批量响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"status\":\"success\"")) {
                status = "SUCCESS";
            } else if (response.contains("\"status\":\"failed\"")) {
                status = "FAILED";
            } else if (response.contains("\"status\":\"sending\"")) {
                status = "SENDING";
            }

            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus(status)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        } catch (Exception e) {
            log.error("解析七牛云SMS查询响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        }
    }
}