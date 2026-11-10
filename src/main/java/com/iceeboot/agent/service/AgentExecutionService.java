package com.iceeboot.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.agent.config.AgentProperties;
import com.iceeboot.agent.model.AgentContracts.*;
import com.iceeboot.agent.persistence.AgentStore;
import com.iceeboot.agent.runtime.*;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class AgentExecutionService {
  private final AgentStore store;
  private final HarnessRuntimeFactory harness;
  private final AgentModelFactory models;
  private final AgentProperties properties;
  private final Semaphore capacity;
  private final Set<String> activeSessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
  private final ObjectMapper json;

  public AgentExecutionService(
      AgentStore store,
      HarnessRuntimeFactory harness,
      AgentModelFactory models,
      AgentProperties properties,
      ObjectMapper json) {
    this.store = store;
    this.harness = harness;
    this.models = models;
    this.properties = properties;
    this.capacity = new Semaphore(properties.getMaxConcurrentRuns());
    this.json = json;
  }

  public InvokeResult invoke(String code, InvokeRequest request, Caller caller) {
    if (!capacity.tryAcquire()) throw new IllegalStateException("当前执行任务较多，请稍后重试");
    List<AutoCloseable> resources = new CopyOnWriteArrayList<>();
    RunRecorder recorder = null;
    String sessionKey = null;
    boolean sessionAcquired = false;
    try {
      AgentView view = store.agentByCode(code);
      PublishedAgent p = store.release(view.activeReleaseId());
      if (!p.agent().enabled()) throw new IllegalArgumentException("智能体发布版本已禁用");
      AtomicInteger budget = new AtomicInteger(100);
      recorder = new RunRecorder(null, "", p, caller, store, budget);
      String session =
          request.sessionId() == null || request.sessionId().isBlank()
              ? recorder.id
              : request.sessionId();
      if (!session.matches("[a-zA-Z0-9_-]{1,100}"))
        throw new IllegalArgumentException("会话编码仅允许字母、数字、下划线和连字符");
      sessionKey = p.releaseId() + ":" + caller.userId() + ":" + session;
      sessionAcquired = activeSessions.add(sessionKey);
      if (!sessionAcquired) throw new IllegalStateException("当前会话正在执行，请等待完成后再次调用");
      List<Msg> input = List.of(new UserMessage(request.input()));
      recorder.start(input);
      var agent = harness.create(p, caller, recorder, budget, resources, session);
      resources.add(agent);
      RuntimeContext context =
          RuntimeContext.builder()
              .userId(caller.userId())
              .sessionId(session)
              .runId(recorder.id)
              .build();
      var result =
          p.agent().outputFormat() == OutputFormat.JSON_SCHEMA
              ? agent.call(input, json.valueToTree(p.agent().outputSchema()), context)
              : agent.call(input, context);
      Msg response = result.block(Duration.ofSeconds(properties.getExecutionTimeoutSeconds()));
      RunDetail detail = store.runDetail(recorder.rootId);
      return new InvokeResult(
          recorder.rootId,
          RunRecorder.text(response),
          detail.totalInputTokens(),
          detail.totalOutputTokens(),
          detail.usageKnown());
    } catch (RuntimeException e) {
      if (recorder != null) recorder.fail();
      throw e;
    } finally {
      for (int i = resources.size() - 1; i >= 0; i--)
        try {
          resources.get(i).close();
        } catch (Exception ignored) {
          /* 终态清理继续释放其他资源，不输出凭据。 */
        }
      if (sessionAcquired) activeSessions.remove(sessionKey);
      capacity.release();
    }
  }

  /** 普通业务直接调用模型，不创建 Agent，也不装配任何工具。 */
  public String invokeModel(ModelInvokeRequest request) {
    ModelConfig m = store.model(request.modelConfigId());
    List<Msg> messages = new ArrayList<>();
    if (request.systemPrompt() != null && !request.systemPrompt().isBlank())
      messages.add(Msg.builder().role(MsgRole.SYSTEM).textContent(request.systemPrompt()).build());
    messages.add(new UserMessage(request.input()));
    StringBuilder result = new StringBuilder();
    models.create(m, null).stream(messages, List.of(), models.options(m, null))
        .doOnNext(
            r ->
                r.getContent().stream()
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .forEach(b -> result.append(b.getText())))
        .blockLast(Duration.ofSeconds(120));
    return result.toString();
  }
}
