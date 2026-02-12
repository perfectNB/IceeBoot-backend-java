/**
 * SMS服务抽象基类
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.iceeboot.common.service.sms.service;

import com.iceeboot.common.service.sms.dto.SMSRequest;
import com.iceeboot.common.service.sms.dto.SMSResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

/**
 * SMS服务抽象基类
 * 提供短信发送的通用模板方法和工具方法，子类只需实现厂商特定的逻辑
 *
 * @author CodeIcee
 * @date 2025-01-24
 */
@Slf4j
public abstract class AbstractSMSService implements SMSService {

    protected final WebClient webClient;

    public AbstractSMSService() {
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
    }

    // ==================== 抽象方法（子类必须实现） ====================

    /**
     * 获取厂商显示名称，如"阿里云短信"
     * @return 厂商显示名称
     */
    public abstract String getProviderDisplayName();

    /**
     * 获取默认端点URL
     * @return 默认端点URL
     */
    public abstract String getDefaultEndpoint();

    /**
     * 获取默认超时时间（秒）
     * @return 默认超时时间
     */
    protected int getDefaultTimeout() {
        return 30;
    }

    /**
     * 构建厂商通用请求参数
     * @param request 短信请求
     * @return 通用参数Map
     */
    protected abstract Map<String, String> buildCommonParams(SMSRequest request);

    /**
     * 生成请求签名
     * @param params 请求参数
     * @param accessKeySecret 密钥
     * @return 签名字符串
     * @throws Exception 签名异常
     */
    protected abstract String generateSignature(Map<String, String> params, String accessKeySecret) throws Exception;

    /**
     * 添加单条短信发送的厂商特定参数
     * @param params 参数Map
     * @param request 短信请求
     */
    protected abstract void addSendSmsParams(Map<String, String> params, SMSRequest request);

    /**
     * 添加批量短信发送的厂商特定参数
     * @param params 参数Map
     * @param request 短信请求
     */
    protected abstract void addBatchSmsParams(Map<String, String> params, SMSRequest request);

    /**
     * 添加状态查询的厂商特定参数
     * @param params 参数Map
     * @param messageId 消息ID
     * @param request 短信请求
     */
    protected abstract void addQueryStatusParams(Map<String, String> params, String messageId, SMSRequest request);

    /**
     * 解析单条短信发送响应
     * @param response 原始响应
     * @param phoneNumber 手机号
     * @param startTime 请求开始时间
     * @return 解析后的响应
     */
    protected abstract SMSResponse parseResponse(String response, String phoneNumber, long startTime);

    /**
     * 解析批量短信发送响应
     * @param response 原始响应
     * @param phoneNumbers 手机号数组
     * @param startTime 请求开始时间
     * @return 解析后的响应
     */
    protected abstract SMSResponse parseBatchResponse(String response, String[] phoneNumbers, long startTime);

    /**
     * 解析状态查询响应
     * @param response 原始响应
     * @param messageId 消息ID
     * @param startTime 请求开始时间
     * @return 解析后的响应
     */
    protected abstract SMSResponse parseQueryResponse(String response, String messageId, long startTime);

    // ==================== 通用接口方法实现 ====================

    @Override
    public Mono<SMSResponse> sendSMS(SMSRequest request) {
        return sendTemplateSMS(request);
    }

    @Override
    public Mono<SMSResponse> sendTemplateSMS(SMSRequest request) {
        return executeSmsRequest(request, "发送短信", "SEND_FAILED",
                (params, req) -> addSendSmsParams(params, req),
                (response, startTime) -> parseResponse(response, request.getPhoneNumber(), startTime));
    }

    @Override
    public Mono<SMSResponse> sendBatchSMS(SMSRequest request) {
        return executeSmsRequest(request, "批量发送", "BATCH_SEND_FAILED",
                (params, req) -> addBatchSmsParams(params, req),
                (response, startTime) -> parseBatchResponse(response, request.getPhoneNumbers(), startTime));
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
        return executeSmsRequest(request, "状态查询", "QUERY_FAILED",
                (params, req) -> addQueryStatusParams(params, messageId, req),
                (response, startTime) -> parseQueryResponse(response, messageId, startTime));
    }

    @Override
    public Mono<SMSResponse> queryBalance(SMSRequest request) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            return SMSResponse.failure("NOT_SUPPORTED", getProviderDisplayName() + "不支持余额查询")
                    .setProvider(getProvider())
                    .setResponseTime(System.currentTimeMillis() - startTime);
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
                log.error("{}配置验证失败: {}", getProviderDisplayName(), e.getMessage());
                return false;
            }
        });
    }

    // ==================== 模板方法 ====================

    /**
     * 执行短信请求的通用模板方法
     *
     * @param request       短信请求
     * @param operation     操作描述（用于日志）
     * @param errorCode     失败时的错误码
     * @param paramCustomizer 参数定制器，用于添加厂商特定参数
     * @param responseParser  响应解析器
     * @return 短信响应
     */
    protected Mono<SMSResponse> executeSmsRequest(SMSRequest request, String operation, String errorCode,
            java.util.function.BiConsumer<Map<String, String>, SMSRequest> paramCustomizer,
            java.util.function.BiFunction<String, Long, SMSResponse> responseParser) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // 构建通用参数
                Map<String, String> params = buildCommonParams(request);

                // 添加厂商特定参数
                paramCustomizer.accept(params, request);

                // 生成签名
                String signature = generateSignature(params, request.getAccessKeySecret());
                params.put("Signature", signature);

                // 发送HTTP请求
                String endpoint = request.getEndpoint() != null ? request.getEndpoint() : getDefaultEndpoint();
                int timeout = request.getTimeout() != null ? request.getTimeout() : getDefaultTimeout();

                String response = webClient.post()
                        .uri(endpoint)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .bodyValue(buildQueryString(params))
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(timeout))
                        .block();

                // 解析响应
                return responseParser.apply(response, startTime);

            } catch (Exception e) {
                log.error("{}{}失败: {}", getProviderDisplayName(), operation, e.getMessage(), e);
                return SMSResponse.failure(errorCode, operation + "失败: " + e.getMessage())
                        .setProvider(getProvider())
                        .setResponseTime(System.currentTimeMillis() - startTime);
            }
        });
    }

    // ==================== 通用工具方法 ====================

    /**
     * 构建URL查询字符串
     * @param params 参数Map
     * @return 查询字符串
     */
    protected String buildQueryString(Map<String, String> params) {
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
     * @param map 参数Map
     * @return JSON字符串
     */
    protected String convertMapToJson(Map<String, String> map) {
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
     * @param array 字符串数组
     * @return JSON字符串
     */
    protected String convertArrayToJson(String[] array) {
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
     * 从JSON字符串中提取指定key的值
     * @param json JSON字符串
     * @param key 键名
     * @return 值
     */
    protected String extractJsonValue(String json, String key) {
        try {
            // 尝试匹配字符串值
            String pattern = "\"" + key + "\":\"([^\"]*)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }

            // 尝试匹配数字值
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

    /**
     * URL百分号编码
     * @param value 待编码的值
     * @return 编码后的值
     */
    protected String percentEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.name())
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
        } catch (Exception e) {
            return value;
        }
    }
}