/**
 * 腾讯云SMS服务实现类
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
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 腾讯云SMS服务实现类
 * 基于腾讯云短信服务API实现短信发送功能
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
@Component
public class TencentSMSService extends AbstractSMSService {

    private static final String PROVIDER_NAME = "tencent";
    private static final String DEFAULT_ENDPOINT = "https://sms.tencentcloudapi.com";
    private static final String API_VERSION = "2021-01-11";
    private static final String SERVICE = "sms";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "腾讯云短信";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected Map<String, String> buildCommonParams(SMSRequest request) {
        // 腾讯云使用JSON请求体+自定义Header，不使用通用参数Map
        return new HashMap<>();
    }

    @Override
    protected String generateSignature(Map<String, String> params, String accessKeySecret) throws Exception {
        // 腾讯云签名在buildHeaders中完成，此处不使用
        return "";
    }

    @Override
    protected void addSendSmsParams(Map<String, String> params, SMSRequest request) {
        // 腾讯云使用JSON请求体，不使用通用参数Map
    }

    @Override
    protected void addBatchSmsParams(Map<String, String> params, SMSRequest request) {
        // 腾讯云使用JSON请求体，不使用通用参数Map
    }

    @Override
    protected void addQueryStatusParams(Map<String, String> params, String messageId, SMSRequest request) {
        // 腾讯云使用JSON请求体，不使用通用参数Map
    }

    // ==================== 覆盖模板方法（腾讯云使用JSON请求体而非表单参数） ====================

    @Override
    public Mono<SMSResponse> sendTemplateSMS(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("PhoneNumberSet", new String[]{request.getPhoneNumber()});
                requestBody.put("SmsSdkAppId", request.getAppId());
                requestBody.put("SignName", request.getSignName());
                requestBody.put("TemplateId", request.getTemplateId());

                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("TemplateParamSet", request.getTemplateParams().values().toArray(new String[0]));
                }

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildTencentHeaders(request, "SendSms", jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();

                String response = webClient.post()
                        .uri(endpoint)
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
                requestBody.put("PhoneNumberSet", request.getPhoneNumbers());
                requestBody.put("SmsSdkAppId", request.getAppId());
                requestBody.put("SignName", request.getSignName());
                requestBody.put("TemplateId", request.getTemplateId());

                if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
                    requestBody.put("TemplateParamSet", request.getTemplateParams().values().toArray(new String[0]));
                }

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildTencentHeaders(request, "SendSms", jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();

                String response = webClient.post()
                        .uri(endpoint)
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
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("SmsSdkAppId", request.getAppId());
                requestBody.put("SerialNo", messageId);
                requestBody.put("BeginTime", System.currentTimeMillis() / 1000 - 86400);
                requestBody.put("EndTime", System.currentTimeMillis() / 1000);
                requestBody.put("Limit", 10);
                requestBody.put("Offset", 0);

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildTencentHeaders(request, "PullSmsSendStatus", jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();

                String response = webClient.post()
                        .uri(endpoint)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(jsonBody)
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
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("SmsSdkAppId", request.getAppId());

                String jsonBody = convertToJson(requestBody);
                Map<String, String> headers = buildTencentHeaders(request, "SendStatusStatistics", jsonBody);

                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();

                String response = webClient.post()
                        .uri(endpoint)
                        .headers(httpHeaders -> headers.forEach(httpHeaders::set))
                        .bodyValue(jsonBody)
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

    // ==================== 腾讯云特有方法 ====================

    /**
     * 构建腾讯云请求头
     */
    private Map<String, String> buildTencentHeaders(SMSRequest request, String action, String payload) throws Exception {
        Map<String, String> headers = new HashMap<>();

        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String date = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("Host", "sms.tencentcloudapi.com");
        headers.put("X-TC-Action", action);
        headers.put("X-TC-Version", API_VERSION);
        headers.put("X-TC-Timestamp", timestamp);
        headers.put("X-TC-Region", request.getRegion() != null ? request.getRegion() : "ap-beijing");

        String authorization = generateTencentSignature(request, action, payload, timestamp, date);
        headers.put("Authorization", authorization);

        return headers;
    }

    /**
     * 生成腾讯云API签名
     */
    private String generateTencentSignature(SMSRequest request, String action, String payload, String timestamp, String date) throws Exception {
        String httpRequestMethod = "POST";
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
                "host:sms.tencentcloudapi.com\n" +
                "x-tc-action:" + action.toLowerCase() + "\n";
        String signedHeaders = "content-type;host;x-tc-action";
        String hashedRequestPayload = sha256Hex(payload);
        String canonicalRequest = httpRequestMethod + "\n" +
                canonicalUri + "\n" +
                canonicalQueryString + "\n" +
                canonicalHeaders + "\n" +
                signedHeaders + "\n" +
                hashedRequestPayload;

        String algorithm = "TC3-HMAC-SHA256";
        String credentialScope = date + "/" + SERVICE + "/tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest);
        String stringToSign = algorithm + "\n" +
                timestamp + "\n" +
                credentialScope + "\n" +
                hashedCanonicalRequest;

        byte[] secretDate = hmacSha256(("TC3" + request.getAccessKeySecret()).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmacSha256(secretDate, SERVICE);
        byte[] secretSigning = hmacSha256(secretService, "tc3_request");
        String signature = bytesToHex(hmacSha256(secretSigning, stringToSign));

        return algorithm + " Credential=" + request.getAccessKeyId() + "/" + credentialScope +
                ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(d);
    }

    private byte[] hmacSha256(byte[] key, String msg) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
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
            if (response.contains("\"SendStatusSet\"")) {
                String serialNo = extractJsonValue(response, "SerialNo");
                String requestId = extractJsonValue(response, "RequestId");

                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setMessageId(serialNo)
                        .setRequestId(requestId)
                        .setPhoneNumber(phoneNumber)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else if (response.contains("\"Error\"")) {
                String errorCode = extractJsonValue(response, "Code");
                String errorMessage = extractJsonValue(response, "Message");

                return SMSResponse.failure(errorCode, errorMessage)
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
            log.error("解析腾讯云SMS响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"SendStatusSet\"")) {
                String requestId = extractJsonValue(response, "RequestId");

                return SMSResponse.success()
                        .setProvider(getProvider())
                        .setRequestId(requestId)
                        .setPhoneNumbers(phoneNumbers)
                        .setStatus("SENT")
                        .setResponseTime(System.currentTimeMillis() - startTime)
                        .setRawResponse(response);
            } else if (response.contains("\"Error\"")) {
                String errorCode = extractJsonValue(response, "Code");
                String errorMessage = extractJsonValue(response, "Message");

                return SMSResponse.failure(errorCode, errorMessage)
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
            log.error("解析腾讯云SMS批量响应失败: {}", e.getMessage(), e);
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
            if (response.contains("\"ReportStatus\":\"SUCCESS\"")) {
                status = "SUCCESS";
            } else if (response.contains("\"ReportStatus\":\"FAIL\"")) {
                status = "FAILED";
            }

            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setMessageId(messageId)
                    .setStatus(status)
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        } catch (Exception e) {
            log.error("解析腾讯云SMS查询响应失败: {}", e.getMessage(), e);
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
            return SMSResponse.success()
                    .setProvider(getProvider())
                    .setStatus("SUCCESS")
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response)
                    .setExtraData(Map.of("balanceInfo", "请查看原始响应数据"));
        } catch (Exception e) {
            log.error("解析腾讯云SMS余额响应失败: {}", e.getMessage(), e);
            return SMSResponse.failure("PARSE_ERROR", "响应解析失败")
                    .setProvider(getProvider())
                    .setResponseTime(System.currentTimeMillis() - startTime)
                    .setRawResponse(response);
        }
    }
}