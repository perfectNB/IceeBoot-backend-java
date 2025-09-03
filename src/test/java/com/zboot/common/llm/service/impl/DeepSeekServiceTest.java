/**
 * DeepSeek大模型服务测试类
 * 测试流式和非流式调用功能
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.zboot.common.llm.service.impl;

import com.zboot.common.llm.dto.LLMMessage;
import com.zboot.common.llm.dto.LLMRequest;
import com.zboot.common.llm.dto.LLMResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * DeepSeek大模型服务测试类
 */
@Slf4j
public class DeepSeekServiceTest {
    
    private DeepSeekService deepSeekService;
    private static final String API_KEY = "sk-450bed5b747b41bc9e915d0040dc318b";
    private static final String BASE_URL = "https://api.deepseek.com";
    
    @BeforeEach
    void setUp() {
        deepSeekService = new DeepSeekService();
    }
    
    /**
     * 测试非流式聊天
     */
    @Test
    void testChat() {
        log.info("开始测试DeepSeek非流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("deepseek-chat")
                    .setUserMessage("你好，请简单介绍一下你自己")
                    .setMaxTokens(100)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = deepSeekService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("非流式响应: {}", response);
            
            // 验证响应
            if (response != null && "deepseek".equals(response.getProvider())) {
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
        log.info("开始测试DeepSeek流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("deepseek-chat")
                    .setUserMessage("火焰鸡是什么")
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(true);
            
            // 执行测试
            Flux<LLMResponse> responseFlux = deepSeekService.chatStream(request);
            
            StringBuilder fullContent = new StringBuilder();
            AtomicInteger chunkCount = new AtomicInteger(0);
            
            // 收集流式响应
            responseFlux
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(response -> {
                        chunkCount.incrementAndGet();
                        log.info("流式响应片段 #{}: {}", chunkCount.get(), response);
                        
                        if (response != null && "deepseek".equals(response.getProvider())) {
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
     * 测试推理模式（深度思考）
     */
    @Test
    void testReasoningMode() {
        log.info("开始测试DeepSeek推理模式...");
        
        try {
            // 构建请求，使用推理模型
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("deepseek-reasoner")
                    .setUserMessage("9.9和9.11哪个更大？请详细分析")
                    .setMaxTokens(200)
                    .setTemperature(0.1)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = deepSeekService.chat(request)
                    .timeout(Duration.ofSeconds(60)) // 推理模式可能需要更长时间
                    .block();
            
            log.info("推理模式响应: {}", response);
            
            if (response != null && "deepseek".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 推理模式调用失败: {}", response.getError());
                } else {
                    // 检查是否有推理内容
                    if (response.getExtraData() != null && 
                        response.getExtraData().containsKey("reasoning_content")) {
                        log.info("🧠 推理过程: {}", response.getExtraData().get("reasoning_content"));
                    }
                    
                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                        log.info("💡 最终答案: {}", 
                                response.getChoices().get(0).getMessage().getContent());
                    }
                    
                    log.info("✅ 推理模式调用成功");
                }
            } else {
                log.error("❌ 推理模式响应异常");
            }
            
        } catch (Exception e) {
            log.error("❌ 推理模式测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试多轮对话
     */
    @Test
    void testMultiTurnChat() {
        log.info("开始测试DeepSeek多轮对话...");
        
        try {
            // 构建多轮对话消息
            List<LLMMessage> messages = new ArrayList<>();
            messages.add(LLMMessage.system("你是一个有用的AI助手"));
            messages.add(LLMMessage.user("我的名字是张三"));
            messages.add(LLMMessage.assistant("你好张三，很高兴认识你！"));
            messages.add(LLMMessage.user("你还记得我的名字吗？"));
            
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("deepseek-chat")
                    .setMessages(messages)
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = deepSeekService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("多轮对话响应: {}", response);
            
            if (response != null && "deepseek".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 多轮对话失败: {}", response.getError());
                } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.info("✅ 多轮对话成功，AI回复: {}", content);
                    
                    // 检查是否记住了名字
                    boolean remembersName = content.toLowerCase().contains("张三") || 
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
     * 测试API密钥验证
     */
    @Test
    void testValidateApiKey() {
        log.info("开始测试API密钥验证...");
        
        try {
            Boolean isValid = deepSeekService.validateApiKey(API_KEY, BASE_URL)
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
            List<String> models = deepSeekService.getSupportedModels(API_KEY, BASE_URL)
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
            String provider = deepSeekService.getProvider();
            log.info("厂商名称: {}", provider);
            assert "deepseek".equals(provider) : "厂商名称应该是deepseek";
            
            // 测试默认模型
            String defaultModel = deepSeekService.getDefaultModel();
            log.info("默认模型: {}", defaultModel);
            assert defaultModel != null && !defaultModel.trim().isEmpty() : "默认模型不能为空";
            
            // 测试最大token限制
            Integer maxTokens = deepSeekService.getMaxTokens("deepseek-chat");
            log.info("最大token限制: {}", maxTokens);
            assert maxTokens != null && maxTokens > 0 : "最大token数必须大于0";
            
            // 测试功能支持
            boolean supportsStreaming = deepSeekService.supportsStreaming();
            boolean supportsSystemPrompt = deepSeekService.supportsSystemPrompt();
            boolean isAvailable = deepSeekService.isAvailable();
            
            log.info("支持流式输出: {}", supportsStreaming);
            log.info("支持系统提示词: {}", supportsSystemPrompt);
            log.info("服务可用: {}", isAvailable);
            
            log.info("✅ 服务基本信息测试完成");
            
        } catch (Exception e) {
            log.error("❌ 服务基本信息测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 运行所有测试的主方法
     */
    public static void main(String[] args) {
        DeepSeekServiceTest test = new DeepSeekServiceTest();
        test.setUp();
        
        log.info("🚀 开始DeepSeek服务完整测试...");
        
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
        
        // 推理模式测试
        test.testReasoningMode();
        
        log.info("🎉 DeepSeek服务测试完成！");
    }
}