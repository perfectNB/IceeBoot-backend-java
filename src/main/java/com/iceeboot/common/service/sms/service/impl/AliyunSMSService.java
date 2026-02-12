/**
 * 阿里云SMS服务实现类
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

import java.nio.charset.StandardCharsets;
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
 * @date 2025-09-24
 */
@Slf4j
@Component
public class AliyunSMSService extends AbstractSMSService {

    private static final String PROVIDER_NAME = SystemConstants.SMSProvider.ALIYUN;
    private static final String DEFAULT_ENDPOINT = "https://dysmsapi.aliyuncs.com";
    private static final String API_VERSION = "2017-05-25";

    @Override
    public String getProvider() {
        return PROVIDER_NAME;
    }

    @Override
    public String getProviderDisplayName() {
        return "阿里云短信";
    }

    @Override
    public String getDefaultEndpoint() {
        return DEFAULT_ENDPOINT;
    }

    @Override
    protected Map<String, String> buildCommonParams(SMSRequest request) {
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

    @Override
    protected String generateSignature(Map<String, String> params, String accessKeySecret) throws Exception {
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

        String stringToSign = "POST&%2F&" + percentEncode(canonicalizedQueryString.toString());

        String key = accessKeySecret + "&";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(signData);
    }

    @Override
    protected void addSendSmsParams(Map<String, String> params, SMSRequest request) {
        params.put("Action", "SendSms");
        params.put("PhoneNumbers", request.getPhoneNumber());
        params.put("SignName", request.getSignName());
        params.put("TemplateCode", request.getTemplateId());

        if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
            params.put("TemplateParam", convertMapToJson(request.getTemplateParams()));
        }
    }

    @Override
    protected void addBatchSmsParams(Map<String, String> params, SMSRequest request) {
        params.put("Action", "SendBatchSms");
        params.put("PhoneNumberJson", convertArrayToJson(request.getPhoneNumbers()));
        params.put("SignNameJson", convertArrayToJson(new String[]{request.getSignName()}));
        params.put("TemplateCode", request.getTemplateId());

        if (request.getTemplateParams() != null && !request.getTemplateParams().isEmpty()) {
            params.put("TemplateParamJson", convertMapToJson(request.getTemplateParams()));
        }
    }

    @Override
    protected void addQueryStatusParams(Map<String, String> params, String messageId, SMSRequest request) {
        params.put("Action", "QuerySendDetails");
        params.put("PhoneNumber", request.getPhoneNumber());
        params.put("BizId", messageId);
        params.put("SendDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        params.put("PageSize", "10");
        params.put("CurrentPage", "1");
    }

    @Override
    protected SMSResponse parseResponse(String response, String phoneNumber, long startTime) {
        try {
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

    @Override
    protected SMSResponse parseBatchResponse(String response, String[] phoneNumbers, long startTime) {
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

    @Override
    protected SMSResponse parseQueryResponse(String response, String messageId, long startTime) {
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
}
