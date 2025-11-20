package com.iceeboot.config.ai.saa;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.iceeboot.common.constant.SystemConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 多模型共存
 *
 * @auther CodeIcee
 * @since 2025-09-24
 */
@Configuration
public class SaaLLMConfig {
    @Value("${spring.ai.dashscope.api-key}")
    private String apiKey;


    @Bean(name = "deepseek")
    public ChatModel deepSeek() {
        return DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder()
                        .apiKey(apiKey)
                        .build())
                .defaultOptions(
                        DashScopeChatOptions.builder().withModel(SystemConstants.SaaModel.DEEPSEEK_MODEL).build())
                .build();
    }

    @Bean(name = "deepseekChatClient")
    public ChatClient deepseekChatClient(@Qualifier("deepseek") ChatModel deepseek,ToolCallbackProvider tools) {
        return ChatClient.builder(deepseek)
                .defaultToolCallbacks(tools.getToolCallbacks())
                .defaultOptions(ToolCallingChatOptions.builder()
                        .model(SystemConstants.SaaModel.DEEPSEEK_MODEL)
                        .build())
                .build();
    }

    @Bean(name = "qwen")
    public ChatModel qwen() {
        return DashScopeChatModel.builder()
                .dashScopeApi(DashScopeApi.builder().apiKey(apiKey).build())
                .defaultOptions(DashScopeChatOptions.builder().withModel(SystemConstants.SaaModel.QWEN_MODEL).build())
                .build();
    }

    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(@Qualifier("qwen") ChatModel qwen,ToolCallbackProvider tools) {
        return ChatClient.builder(qwen)
                .defaultToolCallbacks(tools.getToolCallbacks())
                .defaultOptions(ToolCallingChatOptions.builder()
                        .model(SystemConstants.SaaModel.QWEN_MODEL)
                        .build())
                .build();
    }







}