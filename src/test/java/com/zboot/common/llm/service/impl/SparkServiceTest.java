/**
 * 讯飞星火大模型服务测试类
 * 测试流式和非流式调用功能
 * 
 * @author CodeIcee
 * @date 2025-01-24
 */
package com.zboot.common.llm.service.impl;

import com.zboot.common.service.llm.dto.LLMMessage;
import com.zboot.common.service.llm.dto.LLMRequest;
import com.zboot.common.service.llm.dto.LLMResponse;
import com.zboot.common.service.llm.service.impl.SparkService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 讯飞星火大模型服务测试类
 */
@Slf4j
public class SparkServiceTest {
    
    private SparkService sparkService;
    // HTTP协议的APIKey
    private static final String API_KEY = "sk-";
    // WebSocket协议的认证信息（备用）
    private static final String APPID = "e3dbb357";
    private static final String WS_API_KEY = "008b5828e6edcaf41a011ad88dab999e";
    private static final String API_SECRET = "NjI5NWRkMjJkNDJiZTA4YmIyNDI5ZDZj";
    private static final String BASE_URL = "https://spark-api-open.xf-yun.com/v1";
    
    @BeforeEach
    void setUp() {
        sparkService = new SparkService();
    }
    
    /**
     * 测试非流式聊天
     */
    @Test
    void testChat() {
        log.info("开始测试讯飞星火非流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("generalv3.5")
                    .setUserMessage("你好，请简单介绍一下你自己")
                    .setMaxTokens(100)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = sparkService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("非流式响应: {}", response);
            
            // 验证响应
            if (response != null && "spark".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 非流式调用失败，错误信息: {}", response.getError());
                } else if (response.getChoices() != null && !response.getChoices().isEmpty() 
                        && response.getChoices().get(0).getMessage() != null
                        && response.getChoices().get(0).getMessage().getContent() != null) {
                    log.info("✅ 非流式调用成功，响应内容: {}", 
                            response.getChoices().get(0).getMessage().getContent());
                } else {
                    log.warn("⚠️ 响应格式异常，但服务正常");
                }
            } else {
                log.error("❌ 响应为空或厂商信息错误");
            }
            
        } catch (Exception e) {
            log.error("❌ 测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试流式聊天
     */
    @Test
    void testChatStream() {
        log.info("开始测试讯飞星火流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("generalv3.5")
                    .setUserMessage("请用一句话介绍讯飞星火")
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(true);
            
            // 执行测试
            Flux<LLMResponse> responseFlux = sparkService.chatStream(request);
            
            StringBuilder fullContent = new StringBuilder();
            AtomicInteger chunkCount = new AtomicInteger(0);
            
            // 收集流式响应
            responseFlux
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(response -> {
                        chunkCount.incrementAndGet();
                        log.info("流式响应片段 #{}: {}", chunkCount.get(), response);
                        
                        if (response != null && "spark".equals(response.getProvider())) {
                            if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                var choice = response.getChoices().get(0);
                                if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                                    String content = choice.getDelta().getContent();
                                    fullContent.append(content);
                                    log.info("📝 接收到内容片段: {}", content);
                                }
                            }
                            
                            // 检查是否完成
                            if (Boolean.TRUE.equals(response.getFinished())) {
                                log.info("✅ 流式调用完成，完整内容: {}", fullContent.toString());
                            }
                        } else if (response != null && response.getError() != null) {
                            log.error("❌ 流式调用异常: {}", response.getError());
                        }
                    })
                    .doOnComplete(() -> {
                        log.info("🎯 流式响应完成，共接收到 {} 个数据块", chunkCount.get());
                        log.info("📄 完整响应内容: {}", fullContent.toString());
                    })
                    .doOnError(error -> {
                        log.error("❌ 流式调用发生错误: {}", error.getMessage(), error);
                    })
                    .blockLast(); // 等待流完成
            
        } catch (Exception e) {
            log.error("❌ 流式测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试多轮对话
     */
    @Test
    void testMultiTurnChat() {
        log.info("开始测试讯飞星火多轮对话...");
        
        try {
            // 构建多轮对话消息
            List<LLMMessage> messages = new ArrayList<>();
            messages.add(LLMMessage.system("你是一个有用的AI助手"));
            messages.add(LLMMessage.user("我的名字是赵六"));
            messages.add(LLMMessage.assistant("你好赵六，很高兴认识你！"));
            messages.add(LLMMessage.user("你还记得我的名字吗？"));
            
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("generalv3.5")
                    .setMessages(messages)
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = sparkService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("多轮对话响应: {}", response);
            
            if (response != null && "spark".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 多轮对话失败: {}", response.getError());
                } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.info("✅ 多轮对话成功，AI回复: {}", content);
                    
                    // 检查是否记住了名字
                    boolean remembersName = content.toLowerCase().contains("赵六") || 
                                           content.contains("你的名字") ||
                                           content.contains("记得");
                    
                    if (remembersName) {
                        log.info("🎯 AI成功记住了用户名字");
                    }
                } else {
                    log.warn("⚠️ 多轮对话响应格式异常");
                }
            } else {
                log.error("❌ 多轮对话响应异常");
            }
            
        } catch (Exception e) {
            log.error("❌ 多轮对话测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试不同模型
     */
    @Test
    void testDifferentModels() {
        log.info("开始测试讯飞星火不同模型...");
        
        String[] models = {"generalv3.5", "generalv3", "generalv2"};
        
        for (String model : models) {
            try {
                log.info("测试模型: {}", model);
                
                LLMRequest request = new LLMRequest()
                        .setApiKey(API_KEY)
                        .setBaseUrl(BASE_URL)
                        .setModel(model)
                        .setUserMessage("你是什么模型？")
                        .setMaxTokens(30)
                        .setTemperature(0.7)
                        .setStream(false);
                
                LLMResponse response = sparkService.chat(request)
                        .timeout(Duration.ofSeconds(30))
                        .block();
                
                if (response != null && "spark".equals(response.getProvider())) {
                    if (response.getError() != null) {
                        log.error("❌ 模型 {} 调用失败: {}", model, response.getError());
                    } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        log.info("✅ 模型 {} 调用成功，响应: {}", model, content);
                    }
                } else {
                    log.error("❌ 模型 {} 响应异常", model);
                }
                
            } catch (Exception e) {
                log.error("❌ 模型 {} 测试异常: {}", model, e.getMessage());
            }
        }
    }
    
    /**
     * 测试高级模型功能
     */
    @Test
    void testAdvancedModels() {
        log.info("开始测试讯飞星火高级模型...");
        
        String[] advancedModels = {"4.0Ultra", "pro-128k", "max-32k"};
        
        for (String model : advancedModels) {
            try {
                log.info("测试高级模型: {}", model);
                
                LLMRequest request = new LLMRequest()
                        .setApiKey(API_KEY)
                        .setBaseUrl(BASE_URL)
                        .setModel(model)
                        .setUserMessage("请介绍一下你的能力特点")
                        .setMaxTokens(50)
                        .setTemperature(0.7)
                        .setStream(false);
                
                LLMResponse response = sparkService.chat(request)
                        .timeout(Duration.ofSeconds(45))
                        .block();
                
                if (response != null && "spark".equals(response.getProvider())) {
                    if (response.getError() != null) {
                        log.error("❌ 高级模型 {} 调用失败: {}", model, response.getError());
                    } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        String content = response.getChoices().get(0).getMessage().getContent();
                        log.info("✅ 高级模型 {} 调用成功，响应: {}", model, content);
                    }
                } else {
                    log.error("❌ 高级模型 {} 响应异常", model);
                }
                
            } catch (Exception e) {
                log.error("❌ 高级模型 {} 测试异常: {}", model, e.getMessage());
            }
        }
    }
    
    /**
     * 测试API密钥验证
     */
    @Test
    void testValidateApiKey() {
        log.info("开始测试API密钥验证...");
        
        try {
            Boolean isValid = sparkService.validateApiKey(API_KEY, BASE_URL)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            
            if (Boolean.TRUE.equals(isValid)) {
                log.info("✅ API密钥验证成功");
            } else {
                log.warn("⚠️ API密钥验证失败，可能是密钥无效或网络问题");
            }
            
        } catch (Exception e) {
            log.error("❌ API密钥验证过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试获取支持的模型列表
     */
    @Test
    void testGetSupportedModels() {
        log.info("开始测试获取支持的模型列表...");
        
        try {
            List<String> models = sparkService.getSupportedModels(API_KEY, BASE_URL)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (models != null && !models.isEmpty()) {
                log.info("✅ 成功获取模型列表，共{}个模型", models.size());
                models.forEach(model -> log.info("  - {}", model));
            } else {
                log.error("❌ 获取模型列表失败");
            }
            
        } catch (Exception e) {
            log.error("❌ 获取模型列表过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试服务基本信息
     */
    @Test
    void testServiceInfo() {
        log.info("开始测试服务基本信息...");
        
        try {
            // 测试厂商名称
            String provider = sparkService.getProvider();
            log.info("厂商名称: {}", provider);
            assert "spark".equals(provider) : "厂商名称应该是spark";
            
            // 测试默认模型
            String defaultModel = sparkService.getDefaultModel();
            log.info("默认模型: {}", defaultModel);
            assert defaultModel != null && !defaultModel.trim().isEmpty() : "默认模型不能为空";
            
            // 测试最大token限制
            Integer maxTokens = sparkService.getMaxTokens("generalv3.5");
            log.info("最大token限制: {}", maxTokens);
            assert maxTokens != null && maxTokens > 0 : "最大token数必须大于0";
            
            // 测试功能支持
            boolean supportsStreaming = sparkService.supportsStreaming();
            boolean supportsSystemPrompt = sparkService.supportsSystemPrompt();
            boolean isAvailable = sparkService.isAvailable();
            
            log.info("支持流式输出: {}", supportsStreaming);
            log.info("支持系统提示词: {}", supportsSystemPrompt);
            log.info("服务可用: {}", isAvailable);
            
            log.info("✅ 服务基本信息测试完成");
            
        } catch (Exception e) {
            log.error("❌ 服务基本信息测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试认证信息显示
     */
    @Test
    void testAuthInfo() {
        log.info("开始显示认证信息...");
        
        log.info("HTTP协议认证信息:");
        log.info("  - API Key: {}", API_KEY);
        log.info("  - Base URL: {}", BASE_URL);
        
        log.info("WebSocket协议认证信息（备用）:");
        log.info("  - APPID: {}", APPID);
        log.info("  - API Key: {}", WS_API_KEY);
        log.info("  - API Secret: {}", API_SECRET);
        
        log.info("✅ 认证信息显示完成");
    }
    
    /**
     * 运行所有测试的主方法
     */
    public static void main(String[] args) {
        SparkServiceTest test = new SparkServiceTest();
        test.setUp();
        
        log.info("🚀 开始讯飞星火服务完整测试...");
        
        // 认证信息显示
        test.testAuthInfo();
        
        // 基本信息测试
        test.testServiceInfo();
        
        // API密钥验证
        test.testValidateApiKey();
        
        // 获取模型列表
        test.testGetSupportedModels();
        
        // 非流式聊天测试
        test.testChat();
        
        // 流式聊天测试
        test.testChatStream();
        
        // 多轮对话测试
        test.testMultiTurnChat();
        
        // 不同模型测试
        test.testDifferentModels();
        
        // 高级模型测试
        test.testAdvancedModels();
        
        log.info("🎉 讯飞星火服务测试完成！");
    }
}