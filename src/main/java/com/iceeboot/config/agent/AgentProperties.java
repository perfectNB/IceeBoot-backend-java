package com.iceeboot.config.agent;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "iceeboot.agent")
public class AgentProperties {
    private String encryptionKey = "";
    private String workspace = "./.agentscope/iceeboot";
    private String projectApiBaseUrl = "http://127.0.0.1:9000/iceeboot/api";
    private int executionTimeoutSeconds = 300;
    private int maxConcurrentRuns = 8;
    private E2b e2b = new E2b();

    @Data
    public static class E2b {
        private String apiKey = "";
        private String apiBaseUrl = "";
        private String domain = "";
    }
}
