package com.iceeboot.framework.agent;

import com.iceeboot.framework.dto.update.agent.MemoryRequest;
import com.iceeboot.framework.enums.agent.MemoryScope;
import com.iceeboot.framework.mapper.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.model.agent.memory.MemoryData.*;
import com.iceeboot.framework.service.agent.impl.AgentExecutionServiceImpl;
import com.iceeboot.framework.service.agent.memory.*;
import com.iceeboot.framework.service.agent.runtime.*;
import com.iceeboot.framework.service.agent.support.AgentJsonCodec;
import com.iceeboot.framework.service.system.ISysUserService;
import io.agentscope.core.model.GenerateOptions;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import java.util.*;
import static com.iceeboot.framework.enums.agent.MemoryScope.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgentMemoryIntegrationTest {
    AgentFixture f;
    MemorySettingsService settings;
    MemoryProfileService profiles;
    MemoryChangeService changes;
    AgentMemoryJobMapper jobs;
    AgentModelFactory models;
    AgentRunControl control;
    AgentExecutionServiceImpl chat;
    MemoryExtractionWorker worker;
    AgentView agent;
    long id;

    @BeforeEach void setup() throws Exception {
        f = new AgentFixture();
        var mapper = f.mapper(AgentMemoryMapper.class);
        jobs = f.mapper(AgentMemoryJobMapper.class);
        var tx = new DataSourceTransactionManager(f.jdbc.getDataSource());
        settings = AgentFixture.transactional(f.memorySettings(), tx);
        var json = new AgentJsonCodec(f.json);
        profiles = AgentFixture.transactional(new MemoryProfileService(mapper, jobs, settings, json, f.secrets), tx);
        changes = AgentFixture.transactional(new MemoryChangeService(mapper, jobs, settings, profiles, json, f.secrets), tx);
        models = mock(AgentModelFactory.class);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        control = new AgentRunControl(f.store, f.properties);
        var harness = new HarnessRuntimeFactory(models, f.properties, f.secrets, f.store,
                mock(ISysUserService.class), f.conversations());
        chat = new AgentExecutionServiceImpl(f.store, harness, models, f.properties, f.json, f.quota(),
                control, f.conversations(), f.files());
        worker = new MemoryExtractionWorker(jobs, changes, f.store, f.quota(), models, json);
        agent = f.save("memory", f.model().id(), List.of(), List.of(), List.of(), List.of(), false, null);
        id = agent.draft().id();
    }
    @AfterEach void close() { control.close(); }

    void enable(MemoryScope scope) {
        settings.save(scope, id, new MemoryRequest.PolicyDTO(0, true, false, "提取明确偏好",
                Map.of("language", FieldType.TEXT, "city", FieldType.TEXT)));
    }

    void reply(String value) {
        when(models.create(any(), any())).thenReturn(HarnessExecutionTest.model((messages, tools) ->
                HarnessExecutionTest.text(value, 3, 2)));
    }

    void chat(String message, String session) {
        reply("收到");
        chat.invoke("memory", new InvokeRequest(message, session), new Caller("7", "token"));
    }

    void extract(MemoryScope scope) {
        var updates = new Extraction(List.of(
                new Suggestion(scope, "language", "Java", "我使用 Java"),
                new Suggestion(scope, "city", "上海", "不存在的原文")));
        reply(f.store.json(updates));
        worker.poll();
    }

    @Test void extractsOnceFromOriginalUserMessageAndRecallsAcrossSessionsOnlyForOwner() {
        enable(AGENT_USER);
        f.publish(agent);
        chat("我使用 Java", "one");
        assertThat(profiles.view(AGENT_USER, id, "7").values()).isEmpty();
        extract(AGENT_USER);
        assertThat(profiles.view(AGENT_USER, id, "7").values()).containsExactly(Map.entry("language", "Java"));
        assertThat(profiles.view(AGENT_USER, id, "8").values()).isEmpty();
        assertThat(profiles.view(AGENT_USER, id + 1, "7").values()).isEmpty();
        assertThat(f.jdbc.queryForObject("SELECT profile_ciphertext FROM t_agent_memory_profile", String.class))
                .startsWith("enc:").doesNotContain("Java");
        assertThat(jobs.next(System.currentTimeMillis())).isNull();
        when(models.create(any(), any())).thenReturn(HarnessExecutionTest.model((messages, tools) -> {
            assertThat(f.store.json(messages)).contains("Java");
            return HarnessExecutionTest.text("收到", 1, 1);
        }));
        chat.invoke("memory", new InvokeRequest("你好", "another"), new Caller("7", "token"));
        assertThat(f.conversations().messages(id, "7", "another", Long.MAX_VALUE)).hasSize(2)
                .noneMatch(m -> m.getContent().contains("Java"));
    }

    @Test void forgetBlocksInflightExtractionAndExplicitManualEditRestoresField() {
        enable(AGENT_USER);
        var release = f.publish(agent);
        chat("我使用 Java", "one");
        var before = profiles.view(AGENT_USER, id, "7");
        profiles.edit(AGENT_USER, id, "7", "language", null, before.revision());
        extract(AGENT_USER);
        assertThat(profiles.view(AGENT_USER, id, "7").values()).isEmpty();
        assertThat(f.memoryTurns().prepare(release, "7").targets().getFirst().writable()).doesNotContain("language");
        assertThatThrownBy(() -> profiles.edit(AGENT_USER, id, "7", "language", "Rust", before.revision()))
                .hasMessageContaining("刷新");
        profiles.edit(AGENT_USER, id, "7", "language", "Rust", before.revision() + 1);
        assertThat(f.memoryTurns().prepare(release, "7").context()).contains("Rust");
    }

    @Test void sharedExperienceRequiresReviewAndUserGrantCanBeRevokedWhileJobIsPending() {
        enable(AGENT);
        enable(USER);
        var release = f.publish(agent);
        settings.share("7", id, new MemoryRequest.GrantDTO(0, Set.of("city"), Set.of("language")));
        profiles.edit(USER, 0, "7", "language", "Python", 0);
        profiles.edit(USER, 0, "7", "city", "长沙", 2);
        assertThat(f.memoryTurns().prepare(release, "7").context()).contains("长沙").doesNotContain("Python");
        chat("我使用 Java", "one");
        settings.share("7", id, new MemoryRequest.GrantDTO(1, Set.of(), Set.of()));
        extract(USER);
        assertThat(profiles.view(USER, 0, "7").values()).containsEntry("language", "Python");
        assertThat(f.memoryTurns().prepare(release, "7").context()).isEmpty();
        chat("导出前应检查字段", "one");
        reply(f.store.json(new Extraction(List.of(
                new Suggestion(AGENT, "language", "导出前检查字段", "导出前应检查字段")))));
        worker.poll();
        assertThat(profiles.view(AGENT, id, "7").values()).isEmpty();
        var candidate = changes.pending(AGENT, id, "7").getFirst();
        assertThat(changes.review(AGENT, id, "admin", candidate.id(), true)).isTrue();
        assertThat(f.memoryTurns().prepare(release, "8").context()).contains("导出前检查字段");
    }

    @Test void disablingAndChangingPolicyRespectsPublishedSnapshotAndInvalidatesOldReviews() {
        enable(AGENT);
        var first = f.publish(agent);
        chat("导出前应验算", "one");
        reply(f.store.json(new Extraction(List.of(new Suggestion(AGENT, "city", "导出前验算", "导出前应验算")))));
        worker.poll();
        long candidate = changes.pending(AGENT, id, "ops").getFirst().id();
        settings.save(AGENT, id, new MemoryRequest.PolicyDTO(1, true, true, "新提示词",
                Map.of("language", FieldType.TEXT, "city", FieldType.TEXT, "steps", FieldType.TEXT)));
        assertThat(changes.review(AGENT, id, "ops", candidate, true)).isFalse();
        assertThat(f.memoryTurns().prepare(first, "7").targets().getFirst().writable()).doesNotContain("steps");
        var next = f.publish(agent);
        assertThat(f.memoryTurns().prepare(next, "7").targets().getFirst().writable()).contains("steps");
        settings.save(AGENT, id, new MemoryRequest.PolicyDTO(2, false, true, "停用",
                Map.of("language", FieldType.TEXT, "city", FieldType.TEXT, "steps", FieldType.TEXT)));
        assertThat(f.memoryTurns().prepare(next, "7").targets()).isEmpty();
    }

    @Test void personalApiCannotBeUsedToChangePublicMemoryOrPlatformPolicy() {
        var controller = new com.iceeboot.framework.controller.agent.AgentMemoryController(settings, profiles, changes, f.store);
        assertThatThrownBy(() -> controller.edit(AGENT, id, "city", new MemoryRequest.FieldDTO(0, "任意值")))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        assertThatThrownBy(() -> controller.review(AGENT, id, 1, new MemoryRequest.ReviewDTO(true)))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        assertThatThrownBy(() -> controller.policy(USER, id, new MemoryRequest.PolicyDTO(0, true, false,
                "跨智能体", Map.of("city", FieldType.TEXT))))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        assertThat(settings.rule(USER, id).revision()).isZero();
    }

    @Test void administratorEditsTargetUserWithoutMergingOwnersAndAuditsTheActualActor() {
        enable(USER);
        profiles.editUser("7", "1", "language", "Java", 0);
        assertThat(profiles.view(USER, 0, "7").values()).containsEntry("language", "Java");
        assertThat(profiles.view(USER, 0, "1").values()).isEmpty();
        assertThat(profiles.view(USER, 0, "8").values()).isEmpty();
        assertThat(f.jdbc.queryForObject("SELECT actor_user_id FROM t_agent_memory_change", String.class)).isEqualTo("1");
        assertThat(f.jdbc.queryForObject("SELECT origin FROM t_agent_memory_change", String.class)).isEqualTo("ADMIN");
        assertThat(f.jdbc.queryForObject("SELECT source_user_id FROM t_agent_memory_update_job", String.class)).isEqualTo("7");
        long revision = profiles.view(USER, 0, "7").revision();
        profiles.editUser("7", "1", "language", null, revision);
        assertThat(profiles.view(USER, 0, "7").forgottenFields()).contains("language");
        assertThat(profiles.view(USER, 0, "7").values()).isEmpty();
        assertThatThrownBy(() -> profiles.editUser("7", "1", "language", "Python", revision))
                .hasMessage(com.iceeboot.common.constant.ResponseMessage.Agent.MEMORY_CHANGED);
    }

    @Test void administratorReviewsOnlyTheSelectedUsersCandidatesAndRecordsReviewer() {
        settings.save(USER, 0, new MemoryRequest.PolicyDTO(0, true, true, "提取明确偏好", Map.of("language", FieldType.TEXT)));
        settings.share("7", id, new MemoryRequest.GrantDTO(0, Set.of("language"), Set.of("language")));
        f.publish(agent);
        chat("我使用 Java", "general_profile_review");
        extract(USER);
        var pending = changes.pending(USER, 0, "7");
        assertThat(pending).hasSize(1);
        assertThatThrownBy(() -> changes.reviewUser("8", "1", pending.getFirst().id(), true))
                .hasMessage(com.iceeboot.common.constant.ResponseMessage.Agent.PERMISSION_DENIED);
        assertThat(changes.reviewUser("7", "1", pending.getFirst().id(), true)).isTrue();
        assertThat(profiles.view(USER, 0, "7").values()).containsEntry("language", "Java");
        assertThat(profiles.view(USER, 0, "8").values()).isEmpty();
        assertThat(f.jdbc.queryForObject("SELECT reviewer_user_id FROM t_agent_memory_change WHERE id=?", String.class, pending.getFirst().id())).isEqualTo("1");
    }

    @Test void administratorUsingTheUserManagementEndpointIsAuditedEvenWhenEditingOwnProfile() {
        enable(USER);
        profiles.editUser("1", "1", "language", "Java", 0);
        assertThat(f.jdbc.queryForObject("SELECT origin FROM t_agent_memory_change", String.class)).isEqualTo("ADMIN");
    }

    @Test void staleLeaseCannotCommitAndScheduledInputDoesNotBecomeEvidence() {
        enable(AGENT_USER);
        f.publish(agent);
        reply("收到");
        chat.invoke("memory", new InvokeRequest("调度任务", "schedule"), new Caller("7", null));
        assertThat(jobs.next(System.currentTimeMillis())).isNull();
        chat("我使用 Java", "one");
        long job = jobs.next(100);
        assertThat(jobs.claim(job, 100, 101, "a", "first")).isOne();
        assertThat(jobs.claim(job, 102, 110, "b", "second")).isOne();
        var result = new Extraction(List.of(new Suggestion(AGENT_USER, "language", "Java", "我使用 Java")));
        changes.complete(job, 1, result);
        assertThat(profiles.view(AGENT_USER, id, "7").values()).isEmpty();
        changes.complete(job, 2, result);
        changes.complete(job, 2, result);
        assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_memory_change", Long.class)).isOne();
    }
}
