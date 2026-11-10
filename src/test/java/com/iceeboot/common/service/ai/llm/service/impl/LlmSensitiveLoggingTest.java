package com.iceeboot.common.service.ai.llm.service.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.iceeboot.common.service.ai.llm.dto.LLMRequest;
import com.iceeboot.common.service.ai.llm.service.LLMService;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmSensitiveLoggingTest {
    private static final String SECRET = "test-secret-must-never-be-logged";
    private static final String PRIVATE_CONTENT = "private-user-content";

    @Test
    void shouldNotLogReflectedCredentialsOrUpstreamBodiesInEitherTransportMode() {
        for (LLMService service : services()) {
            Logger logger = (Logger) LoggerFactory.getLogger(service.getClass());
            Level previousLevel = logger.getLevel();
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start(); logger.addAppender(appender); logger.setLevel(Level.DEBUG);
            try {
                WebClient client = WebClient.builder().exchangeFunction(request -> Mono.just(
                        ClientResponse.create(HttpStatus.UNAUTHORIZED)
                                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                .body("{\"error\":{\"message\":\"" + SECRET + " " + PRIVATE_CONTENT + "\"}}")
                                .build())).build();
                ReflectionTestUtils.setField(service, "publicEndpointWebClient", client);
                LLMRequest request = new LLMRequest().setApiKey(SECRET).setBaseUrl("https://model.example")
                        .setPublicEndpointOnly(true).setModel("test-model").setUserMessage("connection check")
                        .setMaxTokens(8).setTimeout(2);
                service.chat(request).block(Duration.ofSeconds(5));
                service.chatStream(request).collectList().block(Duration.ofSeconds(5));
                assertSafeLogs(appender, service.getProvider());
            } finally {
                logger.detachAppender(appender); logger.setLevel(previousLevel); appender.stop();
            }
        }
    }

    @Test
    void shouldNotLogSuccessfulContentOrMalformedPayloadDetails() {
        for (LLMService service : List.of(new ChatGPTService(), new DeepSeekService(), new MoonshotService())) {
            Logger logger = (Logger) LoggerFactory.getLogger(service.getClass());
            Level previousLevel = logger.getLevel();
            ListAppender<ILoggingEvent> appender = new ListAppender<>();
            appender.start(); logger.addAppender(appender); logger.setLevel(Level.DEBUG);
            try {
                String body = "{\"id\":\"test\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\""
                        + PRIVATE_CONTENT + "\"}]}],\"choices\":[{\"message\":{\"content\":\"" + PRIVATE_CONTENT
                        + "\"},\"finish_reason\":\"stop\"}]}";
                ReflectionTestUtils.invokeMethod(service, "parseResponse", body);
                ReflectionTestUtils.invokeMethod(service, "parseResponse", "not-json " + SECRET + " " + PRIVATE_CONTENT);
                ReflectionTestUtils.invokeMethod(service, "parseStreamResponse", "not-json " + SECRET + " " + PRIVATE_CONTENT);
                assertSafeLogs(appender, service.getProvider());
            } finally {
                logger.detachAppender(appender); logger.setLevel(previousLevel); appender.stop();
            }
        }
    }

    private List<LLMService> services() {
        return List.of(new DeepSeekService(), new MoonshotService(), new ZhipuService(), new ChatGPTService());
    }

    private void assertSafeLogs(ListAppender<ILoggingEvent> appender, String provider) {
        for (ILoggingEvent event : appender.list) {
            assertThat(event.getFormattedMessage()).as(provider).doesNotContain(SECRET, PRIVATE_CONTENT);
            assertThat(event.getThrowableProxy()).as("untrusted upstream exception must not be logged").isNull();
        }
    }
}
