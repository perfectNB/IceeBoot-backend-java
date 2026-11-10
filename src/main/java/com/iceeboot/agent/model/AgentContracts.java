package com.iceeboot.agent.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 控制台与执行内核的稳定数据契约。第三方 schema 和参数使用动态字段。 */
public final class AgentContracts {
  private AgentContracts() {}

  public enum Provider {
    GPT,
    CLAUDE,
    GEMINI,
    QWEN,
    DEEPSEEK,
    GLM,
    KIMI,
    MINIMAX
  }

  public enum Protocol {
    OPENAI_CHAT,
    OPENAI_RESPONSES,
    ANTHROPIC,
    GEMINI
  }

  public enum CapabilityKind {
    SKILL,
    TOOL,
    MCP,
    SANDBOX,
    KNOWLEDGE
  }

  public enum ToolKind {
    HTTP,
    SCRIPT
  }

  public enum OutputFormat {
    TEXT,
    JSON_SCHEMA
  }

  public record Generation(
      @DecimalMin("0") @DecimalMax("2") Double temperature,
      @Min(1) @Max(1048576) Integer maxTokens,
      Boolean thinking,
      String reasoningEffort,
      @Min(1024) Integer thinkingBudget,
      Map<String, Object> extra) {}

  public record ModelConfig(
      Long id,
      @NotBlank @Size(max = 120) String name,
      @NotNull Provider provider,
      @NotNull Protocol protocol,
      @NotBlank @Size(max = 1000) String baseUrl,
      String apiKey,
      @NotBlank @Size(max = 200) String model,
      @Min(1024) Integer contextWindow,
      boolean enabled,
      @Valid Generation generation) {}

  public record SkillConfig(String content, Map<String, String> resources, List<Long> toolIds) {}

  public record ToolConfig(
      ToolKind kind,
      String url,
      String method,
      Map<String, String> headers,
      Map<String, Object> inputSchema,
      String script,
      boolean projectApi,
      String permissionCode) {}

  public record McpConfig(String url, Map<String, String> headers, List<String> allowedTools) {}

  public record SandboxConfig(String templateId, @Min(60) @Max(3600) Integer timeoutSeconds) {}

  public record Capability(
      Long id,
      @NotNull CapabilityKind kind,
      @NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}") String code,
      @NotBlank @Size(max = 120) String name,
      @Size(max = 2000) String description,
      boolean enabled,
      SkillConfig skill,
      ToolConfig tool,
      McpConfig mcp,
      @Valid SandboxConfig sandbox) {}

  public record AgentSpec(
      Long id,
      @NotBlank @Pattern(regexp = "[a-zA-Z][a-zA-Z0-9_-]{0,63}") String code,
      @NotBlank @Size(max = 120) String name,
      String description,
      String avatar,
      @NotBlank @Size(max = 100000) String prompt,
      @NotNull Long modelConfigId,
      @Min(1) @Max(100) int maxRounds,
      boolean enabled,
      boolean sandboxEnabled,
      Long sandboxTemplateId,
      boolean planMode,
      @NotNull OutputFormat outputFormat,
      Map<String, Object> outputSchema,
      @Valid Generation generation,
      List<Long> skillIds,
      List<Long> toolIds,
      List<Long> mcpIds,
      List<Long> knowledgeIds,
      List<Long> childAgentIds,
      Map<String, String> environment) {}

  /** 发布快照包含完整资源和成员版本，运行时不读取编辑态替换能力。密钥仍为密文。 */
  public record PublishedAgent(
      String releaseId,
      int version,
      AgentSpec agent,
      ModelConfig model,
      List<Capability> capabilities,
      List<PublishedAgent> children) {}

  public record AgentView(
      AgentSpec draft, String activeReleaseId, int activeVersion, long callCount) {}

  public record ReleaseView(String id, int version, LocalDateTime createdAt, String createdBy) {}

  public record InvokeRequest(
      @NotBlank @Size(max = 100000) String input, @Size(max = 100) String sessionId) {}

  public record ModelInvokeRequest(
      @NotNull Long modelConfigId,
      @NotBlank @Size(max = 100000) String input,
      String systemPrompt) {}

  public record InvokeResult(
      String runId, String output, long inputTokens, long outputTokens, boolean usageKnown) {}

  public record RunView(
      String id,
      String rootRunId,
      String parentRunId,
      Long agentId,
      String agentCode,
      int version,
      String userId,
      String status,
      String input,
      String output,
      String error,
      long inputTokens,
      long outputTokens,
      boolean usageKnown,
      LocalDateTime startedAt,
      LocalDateTime finishedAt) {}

  public record ModelCallView(
      String id,
      String runId,
      String rootRunId,
      String agentCode,
      String model,
      String input,
      String output,
      long inputTokens,
      long outputTokens,
      boolean usageKnown,
      LocalDateTime createdAt) {}

  public record RunDetail(
      List<RunView> agents,
      List<ModelCallView> modelCalls,
      long totalInputTokens,
      long totalOutputTokens,
      boolean usageKnown) {}

  /** 从登录请求捕获身份，异步执行时不依赖 ThreadLocal，也不放入模型上下文。 */
  public record Caller(String userId, String accessToken) {}
}
