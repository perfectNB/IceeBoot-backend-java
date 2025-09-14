/**
 * DeepSeek流式聊天专项测试
 * 用于调试流式响应问题
 * 
 * @author CodeIcee
 * @date 2025-09-03
 */
package com.iceeboot.common.llm.service.impl;

import com.iceeboot.common.service.ai.llm.dto.LLMMessage;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.dto.LLMResponse;
import com.iceeboot.common.service.ai.llm.service.impl.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DeepSeek流式聊天专项测试
 */
@Slf4j
public class DeepSeekStreamTest {
    
    private DeepSeekService deepSeekService;
    private static final String API_KEY = "sk-";
    private static final String BASE_URL = "https://api.deepseek.com";
    
    @BeforeEach
    void setUp() {
        deepSeekService = new DeepSeekService();
        
        // 设置DEBUG日志级别
        ch.qos.logback.classic.Logger rootLogger = 
            (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
    }
    
    /**
     * 测试流式聊天 - 详细调试版本
     */
    @Test
    void testChatStreamDebug() {
        log.info("🚀 开始DeepSeek流式聊天调试测试...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("deepseek-chat")
                    .setUserMessage("请简单说一句话")
                    .setMaxTokens(20)
                    .setTemperature(0.7)
                    .setStream(true);
            
            log.info("📋 请求参数: {}", request);
            
            // 执行测试
            Flux<LLMResponse> responseFlux = deepSeekService.chatStream(request);
            
            StringBuilder fullContent = new StringBuilder();
            AtomicInteger chunkCount = new AtomicInteger(0);
            AtomicInteger validChunkCount = new AtomicInteger(0);
            
            // 收集流式响应
            responseFlux
                    .timeout(Duration.ofSeconds(30))
                    .doOnSubscribe(subscription -> {
                        log.info("🔗 开始订阅流式响应...");
                    })
                    .doOnNext(response -> {
                        int count = chunkCount.incrementAndGet();
                        log.info("📦 接收到响应片段 #{}: {}", count, response);
                        
                        if (response != null) {
                            if (response.getError() != null) {
                                log.error("❌ 响应包含错误: {}", response.getError());
                            } else {
                                validChunkCount.incrementAndGet();
                                
                                if ("deepseek".equals(response.getProvider())) {
                                    log.info("✅ 厂商验证通过: {}", response.getProvider());
                                    
                                    if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                                        var choice = response.getChoices().get(0);
                                        log.info("🎯 Choice信息: index={}, finishReason={}", 
                                                choice.getIndex(), choice.getFinishReason());
                                        
                                        if (choice.getDelta() != null) {
                                            log.info("📝 Delta信息: role={}, content={}", 
                                                    choice.getDelta().getRole(), choice.getDelta().getContent());
                                            
                                            if (choice.getDelta().getContent() != null) {
                                                String content = choice.getDelta().getContent();
                                                fullContent.append(content);
                                                log.info("✨ 累积内容: {}", fullContent.toString());
                                            }
                                        } else {
                                            log.warn("⚠️ Delta为空");
                                        }
                                    } else {
                                        log.warn("⚠️ Choices为空或不存在");
                                    }
                                    
                                    // 检查是否完成
                                    if (Boolean.TRUE.equals(response.getFinished())) {
                                        log.info("🏁 流式调用完成标志检测到");
                                    }
                                } else {
                                    log.warn("⚠️ 厂商信息不匹配: expected=deepseek, actual={}", response.getProvider());
                                }
                            }
                        } else {
                            log.error("❌ 接收到空响应");
                        }
                    })
                    .doOnComplete(() -> {
                        log.info("🎯 流式响应完成！");
                        log.info("📊 统计信息:");
                        log.info("  - 总数据块: {}", chunkCount.get());
                        log.info("  - 有效数据块: {}", validChunkCount.get());
                        log.info("  - 完整内容: '{}'", fullContent.toString());
                        log.info("  - 内容长度: {} 字符", fullContent.length());
                    })
                    .doOnError(error -> {
                        log.error("❌ 流式调用发生错误: {}", error.getMessage(), error);
                        log.error("📊 错误时统计信息:");
                        log.error("  - 已接收数据块: {}", chunkCount.get());
                        log.error("  - 已接收有效数据块: {}", validChunkCount.get());
                    })
                    .doFinally(signalType -> {
                        log.info("🔚 流式响应结束，信号类型: {}", signalType);
                    })
                    .blockLast(); // 等待流完成
            
            // 最终验证
            if (chunkCount.get() == 0) {
                log.error("💥 严重问题：没有接收到任何数据块！");
                log.error("🔍 可能的原因：");
                log.error("  1. API密钥无效");
                log.error("  2. 网络连接问题");
                log.error("  3. DeepSeek API响应格式变化");
                log.error("  4. 流式数据解析逻辑错误");
            } else if (validChunkCount.get() == 0) {
                log.error("💥 数据解析问题：接收到数据但无有效内容！");
            } else if (fullContent.length() == 0) {
                log.error("💥 内容提取问题：有有效数据但无文本内容！");
            } else {
                log.info("✅ 流式聊天测试成功！");
            }
            
        } catch (Exception e) {
            log.error("❌ 流式测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 测试非流式聊天作为对比
     */
    @Test
    void testChatNonStreamForComparison() {
        log.info("🔄 开始非流式聊天对比测试...");
        
        try {
            // 构建请求
            LLMRequest request = new LLMRequest()
                    .setApiKey(API_KEY)
                    .setBaseUrl(BASE_URL)
                    .setModel("deepseek-chat")
                    .setUserMessage("请简单说一句话")
                    .setMaxTokens(20)
                    .setTemperature(0.7)
                    .setStream(false);
            
            // 执行测试
            LLMResponse response = deepSeekService.chat(request)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            log.info("📋 非流式响应: {}", response);
            
            if (response != null && "deepseek".equals(response.getProvider())) {
                if (response.getError() != null) {
                    log.error("❌ 非流式调用失败: {}", response.getError());
                } else if (response.getChoices() != null && !response.getChoices().isEmpty()) {
                    String content = response.getChoices().get(0).getMessage().getContent();
                    log.info("✅ 非流式调用成功，内容: {}", content);
                } else {
                    log.warn("⚠️ 非流式响应格式异常");
                }
            } else {
                log.error("❌ 非流式响应异常");
            }
            
        } catch (Exception e) {
            log.error("❌ 非流式测试过程中发生异常: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 运行调试测试的主方法
     */
    public static void main(String[] args) {
        DeepSeekStreamTest test = new DeepSeekStreamTest();
        test.setUp();
        
        log.info("🔧 开始DeepSeek流式聊天调试...");
        
        // 先测试非流式作为对比
        test.testChatNonStreamForComparison();
        
        log.info("\n" + "=".repeat(50));
        
        // 再测试流式
        test.testChatStreamDebug();
        
        log.info("🎉 调试测试完成！");
    }
}