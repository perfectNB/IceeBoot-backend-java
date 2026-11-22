package com.iceeboot.config.agent;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@org.springframework.validation.annotation.Validated
@ConfigurationProperties(prefix = "iceeboot.agent")
public class AgentProperties {
    @lombok.ToString.Exclude private String encryptionKey = "";

    /** 当前写入使用的密钥标识；更换密钥时必须同时更换标识。 */
    private String encryptionKeyId = "primary";

    /** 只用于读取历史密文，键名为旧 keyId；legacy 对应 enc:v1。 */
    @lombok.ToString.Exclude
    private java.util.Map<String, String> decryptionKeys = new java.util.LinkedHashMap<>();

    @jakarta.validation.constraints.Min(0)
    private int dailyModelRequestLimit = 1000;

    @jakarta.validation.constraints.Min(1)
    private int maxModelRequestsPerRun = 100;

    private String workspace = "./.agentscope/iceeboot";
    private String projectApiBaseUrl = "http://127.0.0.1:9000/iceeboot/api";

    @jakarta.validation.constraints.Min(1)
    private int executionTimeoutSeconds = 300;

    @jakarta.validation.constraints.Min(1)
    private int maxConcurrentRuns = 8;

    /** 定时任务独立线程数，仍受总运行并发限制。 */
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(32)
    private int scheduleConcurrency = 2;

    @jakarta.validation.Valid
    private Context context = new Context();

    @Data
    public static class Context {
        /** 超过阈值时由 SDK 将较早对话摘要化，数据库展示记录不受影响。 */
        @jakarta.validation.constraints.Min(4096)
        private int compactTriggerTokens = 24000;
        @jakarta.validation.constraints.Min(1024)
        private int keepRecentTokens = 8000;
        @jakarta.validation.constraints.Min(1024)
        private int maxToolResultCharacters = 12000;
        @jakarta.validation.constraints.Min(256)
        private int toolPreviewCharacters = 2000;
        @jakarta.validation.constraints.AssertTrue(message = "上下文保留预算和工具预览必须小于各自的触发上限")
        public boolean isBudgetValid() {
            return keepRecentTokens < compactTriggerTokens && toolPreviewCharacters < maxToolResultCharacters;
        }
    }

    private E2b e2b = new E2b();

    @Data
    public static class E2b {
        @lombok.ToString.Exclude private String apiKey = "";
        private String apiBaseUrl = "";
        private String domain = "";
    }
}
