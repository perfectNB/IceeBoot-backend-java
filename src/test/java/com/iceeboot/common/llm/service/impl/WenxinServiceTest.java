/**
 * 文心一言大模型服务测试类
 * 测试OAuth2.0认证、流式和非流式调用功能
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.service.impl.DeepSeekService;
import com.iceeboot.common.service.ai.llm.service.impl.WenxinService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文心一言大模型服务测试类
 */
@Slf4j
@org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named = "RUN_LIVE_LLM_TESTS", matches = "true")
public class WenxinServiceTest {
    
    private WenxinService wenxinService;
    private static final String API_KEY = "";
    private static final String SECRET_KEY = "";
    private static final String BASE_URL = "https://aip.baidubce.com";
    
    @BeforeEach
    void setUp() {
        wenxinService = new WenxinService();
    }
    
    /**
     * 测试非流式聊天
     */
    @Test
    void testChat() {
        log.info("开始测试文心一言非流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setSecretKey(SECRET_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("ernie-bot-turbo")
                    .setUserMessage("你好，请简单介绍一下你自己")
                    .setMaxTokens(100)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = wenxinService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("非流式响应: {}", response);
            
            // 验证响应
            if (response != null && "wenxin".equals(response.getProvider())) {
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
        log.info("开始测试文心一言流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setSecretKey(SECRET_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("ernie-bot-turbo")
                    .setUserMessage("请用一句话介绍文心一言")
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(true);
            
            // 执行测试
            Flux<LLMResponse> responseFlux = wenxinService.chatStream(request);
            
            StringBuilder fullContent = new StringBuilder();
            AtomicInteger chunkCount = new AtomicInteger(0);
            
            // 收集流式响应
            responseFlux
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(response -> {
                        chunkCount.incrementAndGet();
                        log.info("流式响应片段 #{}: {}", chunkCount.get(), response);
                        
                        if (response != null && "wenxin".equals(response.getProvider())) {
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
        log.info("开始测试文心一言多轮对话...");
        
        try {
            // 构建多轮对话消息
            List<LLMMessage> messages = new ArrayList<>();
            messages.add(LLMMessage.system("你是一个有用的AI助手"));
            messages.add(LLMMessage.user("我的名字是李四"));
            messages.add(LLMMessage.assistant("你好李四，很高兴认识你！"));
            messages.add(LLMMessage.user("你还记得我的名字吗？"));
            
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setSecretKey(SECRET_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("ernie-bot-turbo")
                    .setMessages(messages)
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = wenxinService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("多轮对话响应: {}", response);
            
            if (response != null && "wenxin".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 多轮对话失败: {}", response.getError());
                } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.info("✅ 多轮对话成功，AI回复: {}", content);
                    
                    // 检查是否记住了名字
                    boolean remembersName = content.toLowerCase().contains("李四") || 
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
        log.info("开始测试文心一言不同模型...");
        
        String[] models = {"ernie-bot", "ernie-bot-turbo", "ernie-3.5", "ernie-speed"};
        
        for (String model : models) {
            try {
                log.info("测试模型: {}", model);
                
                LLMRequest request = new LLMRequest()
                        .setApiKey(API_KEY)
                        .setSecretKey(SECRET_KEY)
                        .setBaseUrl(BASE_URL)
                        .setModel(model)
                        .setUserMessage("你是什么模型？")
                        .setMaxTokens(30)
                        .setTemperature(0.7)
                        .setStream(false);
                
                LLMResponse response = wenxinService.chat(request)
                        .timeout(Duration.ofSeconds(30))
                        .block();
                
                if (response != null && "wenxin".equals(response.getProvider())) {
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
        log.info("开始测试文心一言高级模型...");
        
        String[] advancedModels = {"ernie-bot-4", "ernie-lite", "ernie-tiny-8k"};
        
        for (String model : advancedModels) {
            try {
                log.info("测试高级模型: {}", model);
                
                LLMRequest request = new LLMRequest()
                        .setApiKey(API_KEY)
                        .setSecretKey(SECRET_KEY)
                        .setBaseUrl(BASE_URL)
                        .setModel(model)
                        .setUserMessage("请介绍一下你的能力特点")
                        .setMaxTokens(50)
                        .setTemperature(0.7)
                        .setStream(false);
                
                LLMResponse response = wenxinService.chat(request)
                        .timeout(Duration.ofSeconds(45))
                        .block();
                
                if (response != null && "wenxin".equals(response.getProvider())) {
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
     * 测试OAuth2.0认证流程
     */
    @Test
    void testOAuth2Authentication() {
        log.info("开始测试OAuth2.0认证流程...");
        
        try {
            // 测试正确的认证信息
            LLMRequest validRequest = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setSecretKey(SECRET_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("ernie-bot-turbo")
                    .setUserMessage("Hello")
                    .setMaxTokens(1);
            
            LLMResponse response = wenxinService.chat(validRequest)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            if (response != null && response.getError() == null) {
                log.info("✅ OAuth2.0认证成功");
            } else {
                log.warn("⚠️ OAuth2.0认证可能失败: {}", response != null ? response.getError() : "响应为空");
            }
            
            // 测试错误的认证信息
            LLMRequest invalidRequest = new LLMRequest()
                    .setApiKey("invalid_key")
                    .setSecretKey("invalid_secret")
                    .setBaseUrl(BASE_URL)
                    .setModel("ernie-bot-turbo")
                    .setUserMessage("Hello")
                    .setMaxTokens(1);
            
            LLMResponse invalidResponse = wenxinService.chat(invalidRequest)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            
            if (invalidResponse != null && invalidResponse.getError() != null) {
                log.info("✅ 错误认证信息正确被拒绝");
            } else {
                log.warn("⚠️ 错误认证信息未被正确处理");
            }
            
        } catch (Exception e) {
            log.error("❌ OAuth2.0认证测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试API密钥验证
     */
    @Test
    void testValidateApiKey() {
        log.info("开始测试API密钥验证...");
        
        try {
            // 测试正确的密钥组合
            String combinedKey = API_KEY + ":" + SECRET_KEY;
            Boolean isValid = wenxinService.validateApiKey(combinedKey, BASE_URL)
                    .timeout(Duration.ofSeconds(15))
                    .block();
            
            if (Boolean.TRUE.equals(isValid)) {
                log.info("✅ API密钥验证成功");
            } else {
                log.warn("⚠️ API密钥验证失败，可能是密钥无效或网络问题");
            }
            
            // 测试错误的密钥格式
            Boolean isInvalid = wenxinService.validateApiKey("invalid_format", BASE_URL)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (Boolean.FALSE.equals(isInvalid)) {
                log.info("✅ 错误密钥格式正确被拒绝");
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
            String combinedKey = API_KEY + ":" + SECRET_KEY;
            List<String> models = wenxinService.getSupportedModels(combinedKey, BASE_URL)
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
            String provider = wenxinService.getProvider();
            log.info("厂商名称: {}", provider);
            assert "wenxin".equals(provider) : "厂商名称应该是wenxin";
            
            // 测试默认模型
            String defaultModel = wenxinService.getDefaultModel();
            log.info("默认模型: {}", defaultModel);
            assert defaultModel != null && !defaultModel.trim().isEmpty() : "默认模型不能为空";
            
            // 测试最大token限制
            Integer maxTokens = wenxinService.getMaxTokens("ernie-bot-turbo");
            log.info("最大token限制: {}", maxTokens);
            assert maxTokens != null && maxTokens > 0 : "最大token数必须大于0";
            
            // 测试功能支持
            boolean supportsStreaming = wenxinService.supportsStreaming();
            boolean supportsSystemPrompt = wenxinService.supportsSystemPrompt();
            boolean isAvailable = wenxinService.isAvailable();
            
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
        
        log.info("OAuth2.0认证信息:");
        log.info("  - API Key: {}", API_KEY);
        log.info("  - Secret Key: {}", SECRET_KEY);
        log.info("  - Base URL: {}", BASE_URL);
        log.info("  - 认证方式: OAuth2.0 (client_credentials)");
        log.info("  - Token缓存: 25天有效期");
        
        log.info("✅ 认证信息显示完成");
    }
    
    /**
     * 运行所有测试的主方法
     */
    public static void main(String[] args) {
        WenxinServiceTest test = new WenxinServiceTest();
        test.setUp();
        
        log.info("🚀 开始文心一言服务完整测试...");
        
        // 认证信息显示
        test.testAuthInfo();
        
        // 基本信息测试
        test.testServiceInfo();
        
        // OAuth2.0认证测试
        test.testOAuth2Authentication();
        
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
        
        log.info("🎉 文心一言服务测试完成！");
    }
}