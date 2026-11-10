package com.iceeboot.config.ai.saa;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.netty.channel.ChannelOption;
import com.iceeboot.common.constant.SystemConstants;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

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

    @Value("${spring.ai.dashscope.read-timeout:660}")
    private Integer readTimeoutSeconds;

    @Value("${iceeboot.ai.dashscope.connect-timeout:10}")
    private Integer connectTimeoutSeconds;

    @Value("${iceeboot.ai.dashscope.max-connections:100}")
    private Integer maxConnections;

    @Value("${iceeboot.ai.dashscope.pending-acquire-timeout:30}")
    private Integer pendingAcquireTimeoutSeconds;

    @Value("${iceeboot.ai.dashscope.max-idle-time:20}")
    private Integer maxIdleTimeSeconds;

    @Value("${iceeboot.ai.dashscope.max-life-time:300}")
    private Integer maxLifeTimeSeconds;

    @Bean(name = "deepseek")
    public ChatModel deepSeek() {
        return DashScopeChatModel.builder()
                .dashScopeApi(buildDashScopeApi())
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
                .dashScopeApi(buildDashScopeApi())
                .defaultOptions(DashScopeChatOptions.builder().withModel(SystemConstants.SaaModel.QWEN_PLUS).build())
                .build();
    }

    @Bean(name = "glm")
    public ChatModel glm() {
        return DashScopeChatModel.builder()
                .dashScopeApi(buildDashScopeApi())
                .defaultOptions(DashScopeChatOptions.builder().withModel(SystemConstants.SaaModel.GLM_47).build())
                .build();
    }

    @Bean(name = "qwenChatClient")
    public ChatClient qwenChatClient(@Qualifier("qwen") ChatModel qwen,ToolCallbackProvider tools) {
        return ChatClient.builder(qwen)
                .defaultToolCallbacks(tools.getToolCallbacks())
                .defaultOptions(ToolCallingChatOptions.builder()
                        .model(SystemConstants.SaaModel.QWEN_PLUS)
                        .build())
                .build();
    }

    @Bean(name = "glmChatClient")
    public ChatClient glmChatClient(@Qualifier("glm") ChatModel glm, ToolCallbackProvider tools) {
        return ChatClient.builder(glm)
                .defaultToolCallbacks(tools.getToolCallbacks())
                .defaultOptions(ToolCallingChatOptions.builder()
                        .model(SystemConstants.SaaModel.GLM_47)
                        .build())
                .build();
    }

    @Bean(name = "saaConnectionProvider", destroyMethod = "dispose")
    public ConnectionProvider saaConnectionProvider() {
        int poolMaxConnections = Math.max(20, maxConnections == null ? 100 : maxConnections);
        int pendingAcquireTimeout = Math.max(3, pendingAcquireTimeoutSeconds == null ? 30 : pendingAcquireTimeoutSeconds);
        int maxIdleTime = Math.max(5, maxIdleTimeSeconds == null ? 20 : maxIdleTimeSeconds);
        int maxLifeTime = Math.max(maxIdleTime + 10, maxLifeTimeSeconds == null ? 300 : maxLifeTimeSeconds);

        return ConnectionProvider.builder("dashscope-chat-provider")
                .maxConnections(poolMaxConnections)
                .pendingAcquireTimeout(Duration.ofSeconds(pendingAcquireTimeout))
                .maxIdleTime(Duration.ofSeconds(maxIdleTime))
                .maxLifeTime(Duration.ofSeconds(maxLifeTime))
                .evictInBackground(Duration.ofSeconds(Math.min(maxIdleTime, 30)))
                .lifo()
                .build();
    }

    private DashScopeApi buildDashScopeApi() {
        int readTimeout = Math.max(10, readTimeoutSeconds == null ? 120 : readTimeoutSeconds);
        int connectTimeout = Math.max(3, connectTimeoutSeconds == null ? 10 : connectTimeoutSeconds);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(connectTimeout));
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeout));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

        HttpClient httpClient = HttpClient.create(saaConnectionProvider())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout * 1000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(Duration.ofSeconds(readTimeout));
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));

        return DashScopeApi.builder()
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .webClientBuilder(webClientBuilder)
                .build();
    }







}
