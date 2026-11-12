package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.impl.AgentExecutionServiceImpl;
import com.iceeboot.framework.service.agent.runtime.*;
import com.iceeboot.framework.service.agent.support.AgentProgressPreview;
import com.iceeboot.framework.service.system.ISysUserService;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import org.junit.jupiter.api.*;
import reactor.core.publisher.*;
import reactor.test.StepVerifier;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class AgentStreamingTest {
    AgentFixture f;
    AgentRunControl control;
    AgentModelFactory models;
    AgentExecutionServiceImpl execution;
    @BeforeEach void setup() throws Exception {
        f = new AgentFixture(); models = mock(AgentModelFactory.class);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        control = new AgentRunControl(f.store, f.properties);
        execution = new AgentExecutionServiceImpl(f.store,
                new HarnessRuntimeFactory(models, f.properties, f.secrets, f.store, mock(ISysUserService.class), f.conversations()),
                models, f.properties, f.json, f.quota(), control, f.conversations(), f.files());
    }
    @AfterEach void close() { control.close(); }
    PublishedAgent publish(String code) {
        return f.publish(f.save(code, f.model().id(), List.of(), List.of(), List.of(), List.of(), false, null));
    }
    Model streaming(Flux<ChatResponse> responses) {
        return new Model() {
            public String getModelName() { return "stream-test"; }
            public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) { return responses; }
        };
    }
    static ChatResponse chunk(String text, String finish) {
        return ChatResponse.builder().id("reply").content(List.of(TextBlock.builder().text(text).build())).finishReason(finish).build();
    }
    @Test void deliversTextBeforeModelFinishesAndPersistsOrderedProgressWithSessionIsolation() {
        PublishedAgent agent = publish("streaming");
        Sinks.Empty<Void> gate = Sinks.empty();
        when(models.create(any(), any())).thenReturn(streaming(Flux.concat(Flux.just(chunk("第一段", null)), gate.asMono().thenMany(Flux.just(chunk("第二段", "stop"))))));
        List<AgentProgress> events = new CopyOnWriteArrayList<>();
        StepVerifier.create(execution.stream("streaming", new InvokeRequest("你好", "stream_session"), new Caller("1", "token")).doOnNext(events::add))
                .thenConsumeWhile(event -> event.type() != AgentProgressType.TEXT_DELTA)
                .assertNext(event -> { assertThat(event.text()).contains("第一段"); assertThat(f.conversations().messages(agent.agent().id(), "1", "stream_session", Long.MAX_VALUE).getLast().getStatus()).isEqualTo("RUNNING"); })
                .then(() -> gate.tryEmitEmpty())
                .thenConsumeWhile(event -> event.type() != AgentProgressType.RUN_FINISHED)
                .assertNext(event -> assertThat(event.text()).isEqualTo("第一段第二段"))
                .expectComplete().verify(Duration.ofSeconds(10));
        String root = events.getFirst().rootRunId();
        var saved = f.conversations().progress(agent.agent().id(), "1", "stream_session", root);
        assertThat(saved).anyMatch(e -> e.type()==AgentProgressType.PHASE_FINISHED && e.callId().equals("ENVIRONMENT") && e.durationMs()!=null);
        assertThat(saved).filteredOn(e -> e.type()==AgentProgressType.MODEL_FINISHED).hasSize(1).allSatisfy(e -> {
            assertThat(e.durationMs()).isNotNegative();assertThat(e.usageKnown()).isFalse();
        });
        assertThat(saved).anyMatch(e -> e.type()==AgentProgressType.PHASE_FINISHED && e.callId().equals("CONTEXT_RESTORE") && e.durationMs()!=null);
        assertThat(saved).isNotEmpty().noneMatch(e -> e.type() == AgentProgressType.TEXT_DELTA);
        assertThat(saved).extracting(AgentProgress::sequence).isSorted().doesNotHaveDuplicates();
        assertThat(f.conversations().messages(agent.agent().id(), "1", "stream_session", Long.MAX_VALUE).getLast().getContent()).isEqualTo("第一段第二段");
        assertThatThrownBy(() -> f.conversations().progress(agent.agent().id(), "2", "stream_session", root)).hasMessageContaining("会话");
        f.conversations().open(agent.agent().id(), "2", "stream_session", agent, "other");
        assertThat(f.conversations().progress(agent.agent().id(), "2", "stream_session", root)).isEmpty();
    }
    @Test void finishedRunDoesNotPublishLateSdkProgress() {
        var agent=publish("late_progress");
        var recorder=new RunRecorder(null,"",agent,new Caller("1","token"),f.store,new AtomicInteger(10));
        var events=new ArrayList<AgentProgress>();recorder.observeProgress(events::add);
        recorder.start(List.of(new UserMessage("task")));
        recorder.beginStage("initial","准备");
        var child=recorder.child(agent,new Caller("1","token"));child.start(List.of(new UserMessage("child task")));
        recorder.finish("CANCELLED","已取消");
        int count=events.size();
        recorder.progress(AgentProgressType.PHASE,"迟到事件","","","","RUNNING");
        recorder.beginStage("late","迟到阶段");recorder.endStage("late","SUCCEEDED");
        child.progress(AgentProgressType.PHASE,"迟到子任务","","","","RUNNING");child.finish("CANCELLED","已取消");
        assertThat(events).hasSize(count);
        assertThat(events.getLast().type()).isEqualTo(AgentProgressType.AGENT_FINISHED);
    }
    @Test void longConversationCompactsThroughMeteredModelWithoutChangingVisibleHistory() {
        f.properties.getContext().setCompactTriggerTokens(4096);
        f.properties.getContext().setKeepRecentTokens(1024);
        var agent = publish("compact_context");
        AtomicInteger summaries = new AtomicInteger();
        when(models.create(any(),any())).thenReturn(HarnessExecutionTest.model((messages,tools) -> {
            if (messages.stream().anyMatch(m -> m.getTextContent().contains("Context Extraction Assistant"))) {
                summaries.incrementAndGet();
                return HarnessExecutionTest.text("历史摘要：保留用户目标和已完成事项",100,20);
            }
            return HarnessExecutionTest.text("已记录",10,2);
        }));
        for (int i=0;i<8;i++) execution.invoke("compact_context",new InvokeRequest("用户事实".repeat(1500)+i,"long_context"),new Caller("1","token"));
        assertThat(summaries.get()).isPositive();
        var history = f.conversations().messages(agent.agent().id(),"1","long_context",Long.MAX_VALUE);
        assertThat(history).hasSize(16);
        assertThat(history.getFirst().getContent()).hasSize(6001);
        var events=f.conversations().progress(agent.agent().id(),"1","long_context",history.getLast().getRunId());
        assertThat(events).anyMatch(e->e.type()==AgentProgressType.PHASE_FINISHED && e.callId().equals("CONTEXT_COMPACTION"));
        assertThat(events).filteredOn(e->e.type()==AgentProgressType.MODEL_FINISHED).hasSize(2);
        assertThat(events).filteredOn(e->e.type()==AgentProgressType.MODEL_FIRST_TOKEN).hasSize(1);
    }
    @Test void cancellationReturnsTerminalStatusAndReleasesSessionWithoutLosingPartialReply() {
        PublishedAgent agent = publish("cancel_stream");
        when(models.create(any(), any())).thenReturn(streaming(Flux.concat(Flux.just(chunk("已经生成的内容", null)), Flux.never())));
        var events = execution.stream("cancel_stream", new InvokeRequest("取消测试", "cancel_session"), new Caller("1", "token"))
                .doOnNext(event -> { if(event.type() == AgentProgressType.TEXT_DELTA) execution.cancel(event.rootRunId()); })
                .collectList().block(Duration.ofSeconds(10));
        assertThat(events.getLast().type()).isEqualTo(AgentProgressType.ERROR);
        assertThat(events.getLast().status()).isEqualTo("CANCELLED");
        var messages = f.conversations().messages(agent.agent().id(), "1", "cancel_session", Long.MAX_VALUE);
        assertThat(messages.getLast().getStatus()).isEqualTo("CANCELLED");
        assertThat(messages.getLast().getContent()).isEqualTo("已经生成的内容");
        assertThat(f.jdbc.queryForObject("SELECT owner FROM t_agent_session WHERE agent_id=? AND user_id=? AND session_id=?", String.class, agent.agent().id(), "1", "cancel_session")).isEmpty();
    }
    @Test void streamingChildRunsShareRootAndHaveDistinctAgentIdentities() {
        var model = f.model();
        var child = f.publish(f.save("child_stream", model.id(), List.of(), List.of(), List.of(), List.of(), false, null));
        f.publish(f.save("parent_stream", model.id(), List.of(), List.of(), List.of(), List.of(child.agent().id()), false, null));
        AtomicInteger calls = new AtomicInteger();
        when(models.create(any(), any())).thenReturn(HarnessExecutionTest.model((messages, tools) ->
                calls.incrementAndGet() == 1 ? HarnessExecutionTest.tool("agent_child_stream", Map.of("task", "获取资料")) : HarnessExecutionTest.text("资料结果", 1, 1)));
        var events = execution.stream("parent_stream", new InvokeRequest("委派任务", "parent_session"), new Caller("1", "token")).collectList().block(Duration.ofSeconds(10));
        assertThat(events).anyMatch(e -> e.type() == AgentProgressType.TOOL_STARTED && e.toolName().equals("agent_child_stream"));
        assertThat(events).anyMatch(e -> e.type() == AgentProgressType.AGENT_FINISHED && e.agentCode().equals("child_stream") && !e.parentRunId().isEmpty() && e.text().contains("资料结果"));
        assertThat(events).extracting(AgentProgress::rootRunId).containsOnly(events.getFirst().rootRunId());
        assertThat(events).extracting(AgentProgress::sequence).isSorted().doesNotHaveDuplicates();
    }
    @Test void sseEndpointFlushesTypedEventsAndProviderAnalysisBeforeCompletion() throws Exception {
        publish("sse_api");
        Sinks.Empty<Void> gate = Sinks.empty();
        ChatResponse thinking = ChatResponse.builder().id("reply").content(List.of(ThinkingBlock.builder().thinking("provider-analysis-text").build())).build();
        when(models.create(any(), any())).thenReturn(streaming(Flux.concat(Flux.just(thinking, chunk("first-piece", null)), gate.asMono().thenMany(Flux.just(chunk("-last", "stop"))))));
        var users = mock(ISysUserService.class);
        when(users.getUserRoles(1L)).thenReturn(List.of("superAdmin"));
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                new com.iceeboot.framework.controller.agent.AgentConsoleController(f.configs, execution, f.store, users,
                        new com.iceeboot.framework.service.agent.convert.AgentApiConverter(f.json), f.conversations()))
                .setControllerAdvice(new com.iceeboot.common.exception.agent.AgentConsoleExceptionHandler()).build();
        try (var tokens = mockStatic(com.iceeboot.common.utils.TokenUtils.class)) {
            tokens.when(com.iceeboot.common.utils.TokenUtils::getCurrentUserId).thenReturn(1L);
            tokens.when(com.iceeboot.common.utils.TokenUtils::getToken).thenReturn("token");
            var result = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/v1/agent-console/agents/by-code/sse_api/stream")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"input\":\"test\",\"sessionId\":\"http_session\"}"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.request().asyncStarted())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("X-Accel-Buffering", "no")).andReturn();
            long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (!result.getResponse().getContentAsString().contains("first-piece") && System.nanoTime() < deadline) Thread.sleep(10);
            assertThat(result.getResponse().getContentAsString()).contains("event:progress", "TEXT_DELTA", "first-piece").contains("REASONING_DELTA", "provider-analysis-text").doesNotContain("RUN_FINISHED");
            gate.tryEmitEmpty();
            result.getAsyncResult(5000);
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
            assertThat(result.getResponse().getContentAsString()).contains("RUN_FINISHED", "first-piece-last", "REASONING_FINISHED", "provider-analysis-text");
        } finally { gate.tryEmitEmpty(); }
    }

    @Test void clientDisconnectCancelsExecutionAndReleasesTheConversationLease() throws Exception {
        PublishedAgent agent = publish("disconnect");
        when(models.create(any(), any())).thenReturn(streaming(Flux.concat(Flux.just(chunk("partial", null)), Flux.never())));
        StepVerifier.create(execution.stream("disconnect", new InvokeRequest("test", "disconnect_session"), new Caller("1", "token")))
                .thenConsumeWhile(event -> event.type() != AgentProgressType.TEXT_DELTA).expectNextCount(1).thenCancel().verify(Duration.ofSeconds(5));
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (f.conversations().messages(agent.agent().id(), "1", "disconnect_session", Long.MAX_VALUE).getLast().getStatus().equals("RUNNING") && System.nanoTime() < deadline) Thread.sleep(10);
        assertThat(f.conversations().messages(agent.agent().id(), "1", "disconnect_session", Long.MAX_VALUE).getLast().getStatus()).isEqualTo("CANCELLED");
        assertThat(f.jdbc.queryForObject("SELECT owner FROM t_agent_session WHERE agent_id=? AND user_id=? AND session_id=?", String.class, agent.agent().id(), "1", "disconnect_session")).isEmpty();
    }

    @Test void disconnectDuringProviderAnalysisPreservesItsPartialContentAndReleasesLease() throws Exception {
        var agent = publish("analysis_disconnect");
        var analysis = ChatResponse.builder().id("analysis-reply").content(List.of(ThinkingBlock.builder().thinking("partial-provider-analysis").build())).build();
        when(models.create(any(),any())).thenReturn(streaming(Flux.concat(Flux.just(analysis),Flux.never())));
        var received = new CopyOnWriteArrayList<AgentProgress>();
        StepVerifier.create(execution.stream("analysis_disconnect",new InvokeRequest("test","analysis_session"),new Caller("1","token")).doOnNext(received::add))
                .thenConsumeWhile(event -> event.type()!=AgentProgressType.REASONING_DELTA).expectNextCount(1).thenCancel().verify(Duration.ofSeconds(5));
        long deadline=System.nanoTime()+Duration.ofSeconds(5).toNanos();
        while(f.conversations().messages(agent.agent().id(),"1","analysis_session",Long.MAX_VALUE).getLast().getStatus().equals("RUNNING") && System.nanoTime()<deadline) Thread.sleep(10);
        assertThat(f.conversations().messages(agent.agent().id(),"1","analysis_session",Long.MAX_VALUE).getLast().getStatus()).isEqualTo("CANCELLED");
        assertThat(f.conversations().progress(agent.agent().id(),"1","analysis_session",received.getFirst().rootRunId()))
                .anyMatch(event -> event.type()==AgentProgressType.REASONING_FINISHED && event.status().equals("CANCELLED") && event.text().equals("partial-provider-analysis"))
                .noneMatch(event -> event.type()==AgentProgressType.REASONING_DELTA);
        assertThat(f.jdbc.queryForObject("SELECT owner FROM t_agent_session WHERE agent_id=? AND user_id=? AND session_id=?",String.class,agent.agent().id(),"1","analysis_session")).isEmpty();
    }

    @Test void exhaustedRoundsExposeTheActualFailureInStreamAndRunLedger() {
        var skill=f.configs.saveCapability(new Capability(null,CapabilityKind.SKILL,"round_skill","轮次测试","测试",true,new SkillConfig("测试技能",Map.of(),List.of()),null,null,null));
        f.publish(f.save("round_limit",f.model().id(),List.of(skill.id()),List.of(),List.of(),List.of(),false,null));
        var calls=new AtomicInteger();
        when(models.create(any(),any())).thenReturn(HarnessExecutionTest.model((messages,tools) -> ChatResponse.builder().id("round-"+calls.incrementAndGet())
                .content(List.of(ToolUseBlock.builder().id("load-"+calls.get()).name("load_skill_through_path").input(Map.of("skillId","round_skill_iceeboot-release","path","SKILL.md")).build()))
                .finishReason("tool_calls").build()));
        var events=execution.stream("round_limit",new InvokeRequest("test","round_session"),new Caller("1","token")).collectList().block(Duration.ofSeconds(10));
        var failure=events.getLast();
        assertThat(failure.type()).isEqualTo(AgentProgressType.ERROR);
        assertThat(failure.text()).contains("最大执行轮次");
        assertThat(f.store.runDetail(failure.rootRunId()).agents()).anySatisfy(run -> {
            assertThat(run.error()).contains("最大执行轮次");
            assertThat(run.errorCode()).isEqualTo("MAX_ROUNDS_EXCEEDED");
        });
    }

    @Test void progressPreviewsHideCredentialFieldsAndLimitResultLength() {
        String preview = AgentProgressPreview.input(Map.of("authorization", "Bearer example-private", "nested", Map.of("apiKey", "private-api", "query", "weather"), "environment", Map.of("DATABASE_PASSWORD", "private-db")));
        assertThat(preview).contains("weather", "已隐藏").doesNotContain("example-private", "private-api", "private-db");
        assertThat(AgentProgressPreview.text("password=private-password token=private-token Bearer private-bearer")).doesNotContain("private-password", "private-token", "private-bearer");
        assertThat(AgentProgressPreview.text("x".repeat(10000))).hasSizeLessThan(2100).contains("截断");
    }
}
