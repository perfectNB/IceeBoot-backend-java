/**
 * 通义千问大模型服务测试类
 * 测试流式和非流式调用功能
 * 
 * @author CodeIcee
 * @date 2025-09-04
 */
package com.iceeboot.common.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.service.impl.DeepSeekService;
import com.iceeboot.common.service.ai.llm.service.impl.QwenService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通义千问大模型服务测试类
 */
@Slf4j
public class QwenServiceTest {
    
    private QwenService qwenService;
    private static final String API_KEY = "sk-";
    private static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    
    @BeforeEach
    void setUp() {
        qwenService = new QwenService();
    }
    
    /**
     * 测试非流式聊天
     */
    @Test
    void testChat() {
        log.info("开始测试通义千问非流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("qwen-plus")
                    .setUserMessage("你好，请简单介绍一下你自己")
                    .setMaxTokens(100)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = qwenService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("非流式响应: {}", response);
            
            // 验证响应
            if (response != null && "qwen".equals(response.getProvider())) {
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
        log.info("开始测试通义千问流式聊天...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("qwen-plus")
                    .setUserMessage("请用一句话介绍通义千问")
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(true);
            
            // 执行测试
            Flux<LLMResponse> responseFlux = qwenService.chatStream(request);
            
            StringBuilder fullContent = new StringBuilder();
            AtomicInteger chunkCount = new AtomicInteger(0);
            
            // 收集流式响应
            responseFlux
                    .timeout(Duration.ofSeconds(30))
                    .doOnNext(response -> {
                        chunkCount.incrementAndGet();
                        log.info("流式响应片段 #{}: {}", chunkCount.get(), response);
                        
                        if (response != null && "qwen".equals(response.getProvider())) {
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
        log.info("开始测试通义千问多轮对话...");
        
        try {
            // 构建多轮对话消息
            List<LLMMessage> messages = new ArrayList<>();
            messages.add(LLMMessage.system("你是一个有用的AI助手"));
            messages.add(LLMMessage.user("我的名字是李四"));
            messages.add(LLMMessage.assistant("你好李四，很高兴认识你！"));
            messages.add(LLMMessage.user("你还记得我的名字吗？"));
            
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("qwen-plus")
                    .setMessages(messages)
                    .setMaxTokens(50)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = qwenService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("多轮对话响应: {}", response);
            
            if (response != null && "qwen".equals(response.getProvider())) {
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
        log.info("开始测试通义千问不同模型...");
        
        String[] models = {"qwen-turbo", "qwen-plus", "qwen-max"};
        
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
                
                LLMResponse response = qwenService.chat(request)
                        .timeout(Duration.ofSeconds(30))
                        .block();
                
                if (response != null && "qwen".equals(response.getProvider())) {
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
     * 测试API密钥验证
     */
    @Test
    void testValidateApiKey() {
        log.info("开始测试API密钥验证...");
        
        try {
            Boolean isValid = qwenService.validateApiKey(API_KEY, BASE_URL)
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
            List<String> models = qwenService.getSupportedModels(API_KEY, BASE_URL)
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
            String provider = qwenService.getProvider();
            log.info("厂商名称: {}", provider);
            assert "qwen".equals(provider) : "厂商名称应该是qwen";
            
            // 测试默认模型
            String defaultModel = qwenService.getDefaultModel();
            log.info("默认模型: {}", defaultModel);
            assert defaultModel != null && !defaultModel.trim().isEmpty() : "默认模型不能为空";
            
            // 测试最大token限制
            Integer maxTokens = qwenService.getMaxTokens("qwen-plus");
            log.info("最大token限制: {}", maxTokens);
            assert maxTokens != null && maxTokens > 0 : "最大token数必须大于0";
            
            // 测试功能支持
            boolean supportsStreaming = qwenService.supportsStreaming();
            boolean supportsSystemPrompt = qwenService.supportsSystemPrompt();
            boolean isAvailable = qwenService.isAvailable();
            
            log.info("支持流式输出: {}", supportsStreaming);
            log.info("支持系统提示词: {}", supportsSystemPrompt);
            log.info("服务可用: {}", isAvailable);
            
            log.info("✅ 服务基本信息测试完成");
            
        } catch (Exception e) {
            log.error("❌ 服务基本信息测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试长上下文模型
     */
    @Test
    void testLongContextModel() {
        log.info("开始测试通义千问长上下文模型...");
        
        try {
            // 构建较长的输入
            StringBuilder longInput = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                longInput.append("这是第").append(i + 1).append("段文本内容。");
            }
            longInput.append("请总结以上内容。");
            
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("qwen-max-longcontext")
                    .setUserMessage(longInput.toString())
                    .setMaxTokens(100)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = qwenService.chat(request)
                    .timeout(Duration.ofSeconds(45))
                    .block();
            
            log.info("长上下文模型响应: {}", response);
            
            if (response != null && "qwen".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 长上下文模型调用失败: {}", response.getError());
                } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.info("✅ 长上下文模型调用成功，响应: {}", content);
                } else {
                    log.warn("⚠️ 长上下文模型响应格式异常");
                }
            } else {
                log.error("❌ 长上下文模型响应异常");
            }
            
        } catch (Exception e) {
            log.error("❌ 长上下文模型测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 运行所有测试的主方法
     */
    public static void main(String[] args) {
        QwenServiceTest test = new QwenServiceTest();
        test.setUp();
        
        log.info("🚀 开始通义千问服务完整测试...");
        
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
        
        // 长上下文模型测试
        test.testLongContextModel();
        
        log.info("🎉 通义千问服务测试完成！");
    }
}