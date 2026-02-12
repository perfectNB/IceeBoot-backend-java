package com.iceeboot.common.service.ai.llm.service.impl;

import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.common.service.ai.llm.dto.LLMChoice;
import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 讯飞星火大模型WebSocket协议服务实现类（示例）
 * 
 * 注意：这是一个示例实现，展示如何使用WebSocket协议调用讯飞星火API
 * 实际实现需要使用WebSocket客户端库（如Java-WebSocket）
 */
@Slf4j
public class SparkWebSocketService implements LLMService {
    
    // WebSocket协议的认证信息
    private String appId;
    private String apiKey;
    private String apiSecret;
    
    // 不同版本的WebSocket URL
    private static final String SPARK_LITE_URL = "wss://spark-api.xf-yun.com/v1.1/chat";
    private static final String SPARK_PRO_URL = "wss://spark-api.xf-yun.com/v3.1/chat";
    private static final String SPARK_MAX_URL = "wss://spark-api.xf-yun.com/v3.5/chat";
    private static final String SPARK_ULTRA_URL = "wss://spark-api.xf-yun.com/v4.0/chat";
    
    private static final String DEFAULT_MODEL = "generalv3.5";
    
    public SparkWebSocketService(String appId, String apiKey, String apiSecret) {
        this.appId = appId;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    @Override
    public String getProvider() {
        return SystemConstants.LLMProvider.SPARK;
    }
    
    @Override
    public Mono<LLMResponse> chat(LLMRequest request) {
        log.info("讯飞星火WebSocket非流式聊天 - 这是示例实现");
        log.info("实际实现需要使用WebSocket客户端库建立连接");
        
        // 示例：返回说明信息
        return Mono.just(createInfoResponse(
            "讯飞星火WebSocket协议示例实现。" +
            "实际使用需要：1) 使用WebSocket客户端库 2) 实现URL鉴权 3) 处理WebSocket消息流"
        ));
    }
    
    @Override
    public Flux<LLMResponse> chatStream(LLMRequest request) {
        log.info("讯飞星火WebSocket流式聊天 - 这是示例实现");
        log.info("WebSocket协议天然支持流式输出");
        
        // 示例：返回说明信息
        return Flux.just(createInfoResponse(
            "讯飞星火WebSocket流式协议示例。" +
            "WebSocket协议通过消息流天然支持实时流式输出。"
        ));
    }
    
    @Override
    public Mono<Boolean> validateApiKey(String apiKey, String baseUrl) {
        log.info("WebSocket协议使用APPID、APIKey、APISecret三元组认证");
        return Mono.just(true); // 示例返回
    }
    
    @Override
    public Mono<List<String>> getSupportedModels(String apiKey, String baseUrl) {
        List<String> models = Arrays.asList(
                "lite",           // Spark Lite
                "generalv3",     // Spark Pro  
                "pro-128k",      // Spark Pro-128K
                "generalv3.5",   // Spark Max
                "max-32k",       // Spark Max-32K
                "4.0Ultra"       // Spark 4.0 Ultra
        );
        return Mono.just(models);
    }
    
    @Override
    public String getDefaultModel() {
        return DEFAULT_MODEL;
    }
    
    @Override
    public Integer getMaxTokens(String model) {
        if (model != null) {
            switch (model) {
                case "lite":
                    return 4096;
                case "generalv3":
                    return 8192;
                case "pro-128k":
                    return 131072;
                case "generalv3.5":
                    return 8192;
                case "max-32k":
                    return 32768;
                case "4.0Ultra":
                    return 32768;
            }
        }
        return 8192;
    }
    
    /**
     * 生成WebSocket连接的鉴权URL
     * 这是讯飞星火WebSocket协议的关键部分
     */
    private String generateAuthUrl(String baseUrl) {
        try {
            URL url = new URL(baseUrl);
            
            // 生成时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            String date = sdf.format(new Date());
            
            // 构建签名字符串
            String signatureOrigin = "host: " + url.getHost() + "\n";
            signatureOrigin += "date: " + date + "\n";
            signatureOrigin += "GET " + url.getPath() + " HTTP/1.1";
            
            // 使用HMAC-SHA256生成签名
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(spec);
            byte[] hexDigits = mac.doFinal(signatureOrigin.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hexDigits);
            
            // 构建authorization字符串
            String authorization = String.format(
                "api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"",
                apiKey, "hmac-sha256", "host date request-line", signature
            );
            
            String authorizationBase64 = Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8));
            
            // 构建最终的WebSocket URL
            return baseUrl + "?authorization=" + authorizationBase64 + "&date=" + 
                   java.net.URLEncoder.encode(date, StandardCharsets.UTF_8) + "&host=" + url.getHost();
                   
        } catch (Exception e) {
            log.error("生成讯飞星火WebSocket鉴权URL失败: {}", e.getMessage());
            return baseUrl;
        }
    }
    
    /**
     * 构建WebSocket请求消息
     */
    private Map<String, Object> buildWebSocketMessage(LLMRequest request) {
        Map<String, Object> message = new HashMap<>();
        
        // header部分
        Map<String, Object> header = new HashMap<>();
        header.put("app_id", appId);
        header.put("uid", "user_" + System.currentTimeMillis());
        message.put("header", header);
        
        // parameter部分
        Map<String, Object> parameter = new HashMap<>();
        Map<String, Object> chat = new HashMap<>();
        
        String domain = getModelDomain(request.getModel());
        chat.put("domain", domain);
        
        if (request.getTemperature() != null) {
            chat.put("temperature", request.getTemperature());
        }
        if (request.getMaxTokens() != null) {
            chat.put("max_tokens", request.getMaxTokens());
        }
        
        parameter.put("chat", chat);
        message.put("parameter", parameter);
        
        // payload部分
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> messageObj = new HashMap<>();
        
        List<Map<String, String>> text = new ArrayList<>();
        
        // 添加系统消息
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", request.getSystemPrompt());
            text.add(systemMsg);
        }
        
        // 添加历史消息
        if (request.getMessages() != null && !request.getMessages().isEmpty()) {
            for (LLMMessage msg : request.getMessages()) {
                Map<String, String> msgMap = new HashMap<>();
                msgMap.put("role", msg.getRole());
                msgMap.put("content", msg.getContent());
                text.add(msgMap);
            }
        } else if (request.getUserMessage() != null) {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getUserMessage());
            text.add(userMsg);
        }
        
        messageObj.put("text", text);
        payload.put("message", messageObj);
        message.put("payload", payload);
        
        return message;
    }
    
    /**
     * 根据模型名称获取对应的domain参数
     */
    private String getModelDomain(String model) {
        if (model == null) {
            return "generalv3.5";
        }
        
        switch (model) {
            case "lite":
                return "lite";
            case "generalv3":
                return "generalv3";
            case "pro-128k":
                return "pro-128k";
            case "generalv3.5":
                return "generalv3.5";
            case "max-32k":
                return "max-32k";
            case "4.0Ultra":
                return "4.0Ultra";
            default:
                return "generalv3.5";
        }
    }
    
    /**
     * 根据模型获取对应的WebSocket URL
     */
    private String getWebSocketUrl(String model) {
        if (model == null) {
            return SPARK_MAX_URL;
        }
        
        switch (model) {
            case "lite":
                return SPARK_LITE_URL;
            case "generalv3":
            case "pro-128k":
                return SPARK_PRO_URL;
            case "generalv3.5":
            case "max-32k":
                return SPARK_MAX_URL;
            case "4.0Ultra":
                return SPARK_ULTRA_URL;
            default:
                return SPARK_MAX_URL;
        }
    }
    
    /**
     * 创建信息响应
     */
    private LLMResponse createInfoResponse(String message) {
        LLMResponse response = new LLMResponse()
                .setProvider(getProvider())
                .setFinished(true);
        
        LLMChoice choice = new LLMChoice();
        choice.setIndex(0);
        choice.setFinishReason("stop");
        
        LLMMessage llmMessage = new LLMMessage();
        llmMessage.setRole("assistant");
        llmMessage.setContent(message);
        choice.setMessage(llmMessage);
        
        response.setChoices(Arrays.asList(choice));
        
        return response;
    }
    
    /**
     * 使用示例
     */
    public static void main(String[] args) {
        log.info("讯飞星火WebSocket协议实现示例");
        log.info("实际使用步骤：");
        log.info("1. 添加WebSocket客户端依赖（如Java-WebSocket）");
        log.info("2. 使用generateAuthUrl()生成鉴权URL");
        log.info("3. 建立WebSocket连接");
        log.info("4. 发送buildWebSocketMessage()构建的消息");
        log.info("5. 处理WebSocket返回的流式消息");
        log.info("6. 解析JSON响应并转换为LLMResponse格式");
        
        // 示例认证信息
        String appId = "e3dbb357";
        String apiKey = "";
        String apiSecret = "";
        
        SparkWebSocketService service = new SparkWebSocketService(appId, apiKey, apiSecret);
        
        // 生成鉴权URL示例
        String authUrl = service.generateAuthUrl(SPARK_MAX_URL);
        log.info("生成的鉴权URL: {}", authUrl);
        
        // 构建消息示例
        LLMRequest request = new LLMRequest()
                .setModel("generalv3.5")
                .setUserMessage("你好，请介绍一下你自己")
                .setMaxTokens(100)
                .setTemperature(0.7);
        
        Map<String, Object> wsMessage = service.buildWebSocketMessage(request);
        log.info("WebSocket消息结构已构建完成");
    }
}