package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.*;
import com.iceeboot.framework.service.agent.impl.AgentExecutionServiceImpl;
import com.iceeboot.framework.service.agent.runtime.*;
import com.iceeboot.framework.service.system.ISysUserService;

import io.agentscope.core.message.*;
import io.agentscope.core.model.*;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import javax.crypto.*;
import javax.crypto.spec.*;

class AgentOperationsTest {
    @Test
    void rotationReencryptsModelsDeletedRowsAndNestedSnapshotsWithoutChangingPublishedBehavior()
            throws Exception {
        var f = new AgentFixture();
        var model = f.model();
        var child =
                f.save(
                        "child",
                        model.id(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        null);
        f.publish(child);
        var parent =
                f.publish(
                        f.save(
                                "parent",
                                model.id(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(child.draft().id()),
                                false,
                                null));
        f.store.deleteModel(model.id());
        String old = f.properties.getEncryptionKey();
        byte[] replacement = new byte[32];
        Arrays.fill(replacement, (byte) 7);
        f.properties.getDecryptionKeys().put("primary", old);
        f.properties.setEncryptionKeyId("next");
        f.properties.setEncryptionKey(Base64.getEncoder().encodeToString(replacement));
        var result = f.rotation().rotate();
        assertThat(result.getUpdatedRows()).isEqualTo(5);
        assertThat(result.getActiveKeyId()).isEqualTo("next");
        f.properties.getDecryptionKeys().remove("primary");
        String stored =
                f.jdbc.queryForObject(
                        "SELECT api_key FROM t_agent_model WHERE id=?", String.class, model.id());
        assertThat(f.secrets.decrypt(stored)).isEqualTo("test-key");
        var snapshot = f.store.release(parent.releaseId());
        assertThat(f.secrets.decrypt(snapshot.model().apiKey())).isEqualTo("test-key");
        assertThat(f.secrets.decrypt(snapshot.children().getFirst().model().apiKey()))
                .isEqualTo("test-key");
        assertThat(snapshot.agent()).isEqualTo(parent.agent());
        assertThat(snapshot.version()).isEqualTo(parent.version());
    }

    @Test
    void legacyCiphertextRemainsReadableAndRotatesToIdentifiedKey() throws Exception {
        var f = new AgentFixture();
        byte[] nonce = new byte[12];
        new java.security.SecureRandom().nextBytes(nonce);
        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(new byte[32], "AES"),
                new GCMParameterSpec(128, nonce));
        byte[] encrypted =
                cipher.doFinal("legacy-key".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        byte[] payload = new byte[nonce.length + encrypted.length];
        System.arraycopy(nonce, 0, payload, 0, nonce.length);
        System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
        String legacy = "enc:v1:" + Base64.getEncoder().encodeToString(payload);
        assertThat(f.secrets.decrypt(legacy)).isEqualTo("legacy-key");
        assertThat(f.secrets.reencrypt(legacy)).startsWith("enc:v2:primary:");
        String current = f.secrets.encrypt("x");
        f.properties.setEncryptionKeyId("new");
        assertThatThrownBy(() -> f.secrets.decrypt(current))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SECRET_KEY_VERSION_MISSING);
    }

    @Test
    void corruptedLaterSecretRollsBackEveryEarlierRotationWrite() throws Exception {
        var f = new AgentFixture();
        var m = f.model();
        var t = f.http("tool", "https://example.invalid");
        String before = f.store.model(m.id()).apiKey();
        f.jdbc.update(
                "UPDATE t_agent_tool SET headers_json=? WHERE id=?",
                "{\"Authorization\":\"not-ciphertext\"}",
                t.id());
        assertThatThrownBy(() -> f.rotation().rotate()).isInstanceOf(IceeBootException.class);
        assertThat(f.store.model(m.id()).apiKey()).isEqualTo(before);
    }

    @Test
    void dailyQuotaIsAtomicAcrossConcurrentRequestsAndIndependentAcrossUsers() throws Exception {
        var f = new AgentFixture();
        f.properties.setDailyModelRequestLimit(3);
        var quota = f.quota();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 20; i++)
                futures.add(
                        pool.submit(
                                () -> {
                                    try {
                                        quota.acquire("1");
                                        return true;
                                    } catch (IceeBootException e) {
                                        assertThat(e.getCode()).isEqualTo(429);
                                        return false;
                                    }
                                }));
            int success = 0;
            for (var future : futures) if (future.get(10, TimeUnit.SECONDS)) success++;
            assertThat(success).isEqualTo(3);
            assertThat(quota.getQuota("1").getUsedRequests()).isEqualTo(3);
            assertThat(quota.getQuota("1").getRemainingRequests()).isZero();
            quota.acquire("2");
            assertThat(quota.getQuota("2").getUsedRequests()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void cancellationFromAnotherNodeStopsModelAndReleasesSessionAndCapacity() throws Exception {
        var f = new AgentFixture();
        f.properties.setMaxConcurrentRuns(1);
        var m = f.model();
        f.publish(
                f.save("cancel", m.id(), List.of(), List.of(), List.of(), List.of(), false, null));
        var models = mock(AgentModelFactory.class);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        var subscribed = new CountDownLatch(1);
        var cancelled = new AtomicBoolean();
        Model slow = mock(Model.class);
        when(slow.getModelName()).thenReturn("slow");
        when(slow.getContextWindowSize()).thenReturn(16384);
        when(slow.stream(any(), any(), any()))
                .thenReturn(
                        Flux.<ChatResponse>never()
                                .doOnSubscribe(x -> subscribed.countDown())
                                .doOnCancel(() -> cancelled.set(true)));
        when(models.create(any(), any())).thenReturn(slow);
        var pool = Executors.newSingleThreadExecutor();
        try (var first = new AgentRunControl(f.store, f.properties);
                var other = new AgentRunControl(f.store, f.properties)) {
            var service =
                    new AgentExecutionServiceImpl(
                            f.store,
                            new HarnessRuntimeFactory(
                                    models,
                                    f.properties,
                                    f.secrets,
                                    f.store,
                                    mock(ISysUserService.class)),
                            models,
                            f.properties,
                            f.json,
                            f.quota(),
                            first);
            var result =
                    pool.submit(
                            () ->
                                    service.invoke(
                                            "cancel",
                                            new InvokeRequest("start", "same_session"),
                                            new Caller("1", "token")));
            assertThat(subscribed.await(5, TimeUnit.SECONDS)).isTrue();
            String id = f.store.runs().getFirst().id();
            other.cancel(id);
            assertThatThrownBy(() -> result.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class);
            assertThat(cancelled).isTrue();
            assertThat(f.store.runDetail(id).agents().getFirst().status()).isEqualTo("CANCELLED");
            when(models.create(any(), any()))
                    .thenReturn(
                            HarnessExecutionTest.model(
                                    (messages, tools) -> HarnessExecutionTest.text("done", 2, 1)));
            assertThat(
                            service.invoke(
                                            "cancel",
                                            new InvokeRequest("again", "same_session"),
                                            new Caller("1", "token"))
                                    .output())
                    .isEqualTo("done");
            assertThatThrownBy(() -> other.cancel(id))
                    .hasMessage(ResponseMessage.Agent.CANCEL_NOT_RUNNING);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void directModelUsesSameLedgerQuotaAndSafeFailureDiagnostics() throws Exception {
        var f = new AgentFixture();
        var m = f.model();
        var models = mock(AgentModelFactory.class);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        when(models.create(any(), any()))
                .thenReturn(
                        HarnessExecutionTest.model(
                                (messages, tools) -> HarnessExecutionTest.text("answer", 10, 4)));
        try (var control = new AgentRunControl(f.store, f.properties)) {
            var service =
                    new AgentExecutionServiceImpl(
                            f.store,
                            mock(HarnessRuntimeFactory.class),
                            models,
                            f.properties,
                            f.json,
                            f.quota(),
                            control);
            assertThat(
                            service.invokeModel(
                                    new ModelInvokeRequest(m.id(), "hello", null),
                                    new Caller("1", "token")))
                    .isEqualTo("answer");
            var run = f.store.runs().getFirst();
            var detail = f.store.runDetail(run.id());
            assertThat(run.status()).isEqualTo("SUCCEEDED");
            assertThat(run.output()).isEqualTo("answer");
            assertThat(detail.totalInputTokens()).isEqualTo(10);
            assertThat(detail.totalOutputTokens()).isEqualTo(4);
            assertThat(f.quota().getQuota("1").getUsedRequests()).isEqualTo(1);
            var failure = mock(Model.class);
            when(failure.getModelName()).thenReturn("failed");
            when(failure.stream(any(), any(), any()))
                    .thenReturn(
                            Flux.error(new IllegalArgumentException("api_key=never-save-this")));
            when(models.create(any(), any())).thenReturn(failure);
            assertThatThrownBy(
                            () ->
                                    service.invokeModel(
                                            new ModelInvokeRequest(m.id(), "fail", null),
                                            new Caller("1", "token")))
                    .isInstanceOf(IceeBootException.class);
            var failed =
                    f.store.runs().stream()
                            .filter(r -> r.status().equals("FAILED"))
                            .findFirst()
                            .orElseThrow();
            assertThat(failed.errorCode()).isEqualTo("MODEL_FAILED");
            assertThat(failed.traceId()).isNotBlank();
            assertThat(failed.error()).doesNotContain("never-save-this");
        }
    }

    @Test
    void lostHeartbeatRecoveryLeavesLiveRunsUntouched() throws Exception {
        var f = new AgentFixture();
        var p =
                f.publish(
                        f.save(
                                "recover",
                                f.model().id(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                false,
                                null));
        f.store.startRun("stale", "stale", "", p, "1", "[]");
        f.store.startRun("live", "live", "", p, "1", "[]");
        f.jdbc.update(
                "UPDATE t_agent_run SET heartbeat_at=? WHERE id='stale'",
                java.time.LocalDateTime.now().minusHours(1));
        try (var control = new AgentRunControl(f.store, f.properties)) {
            assertThat(control.recoverStale()).isEqualTo(1);
        }
        assertThat(f.store.runDetail("stale").agents().getFirst().status())
                .isEqualTo("INTERRUPTED");
        assertThat(f.store.runDetail("live").agents().getFirst().status()).isEqualTo("RUNNING");
    }

    @Test
    void timeoutIsRecordedSeparatelyFromUserCancellation() throws Exception {
        var f = new AgentFixture();
        f.properties.setExecutionTimeoutSeconds(1);
        var model = f.model();
        var models = mock(AgentModelFactory.class);
        var stalled = mock(Model.class);
        when(models.create(any(), any())).thenReturn(stalled);
        when(models.options(any(), any())).thenReturn(GenerateOptions.builder().build());
        when(stalled.getModelName()).thenReturn("stalled");
        when(stalled.stream(any(), any(), any())).thenReturn(Flux.never());
        try (var control = new AgentRunControl(f.store, f.properties)) {
            var service =
                    new AgentExecutionServiceImpl(
                            f.store,
                            mock(HarnessRuntimeFactory.class),
                            models,
                            f.properties,
                            f.json,
                            f.quota(),
                            control);
            assertThatThrownBy(
                            () ->
                                    service.invokeModel(
                                            new ModelInvokeRequest(model.id(), "timeout", null),
                                            new Caller("1", "token")))
                    .isInstanceOfSatisfying(
                            IceeBootException.class, e -> assertThat(e.getCode()).isEqualTo(504));
            var run = f.store.runs().getFirst();
            assertThat(run.status()).isEqualTo("TIMED_OUT");
            assertThat(run.errorCode()).isEqualTo("TIMEOUT");
            assertThat(run.traceId()).isNotBlank();
        }
    }

    @Test
    void maskedEditCannotWriteAnOldKeyVersionBackAfterRotation() throws Exception {
        var f = new AgentFixture();
        String old = f.secrets.encrypt("same-provider-key");
        f.properties.getDecryptionKeys().put("primary", f.properties.getEncryptionKey());
        byte[] replacement = new byte[32];
        Arrays.fill(replacement, (byte) 8);
        f.properties.setEncryptionKeyId("next");
        f.properties.setEncryptionKey(Base64.getEncoder().encodeToString(replacement));
        String merged =
                f.secrets.merge(
                        com.iceeboot.framework.service.agent.security.AgentSecrets.MASK, old);
        assertThat(merged).startsWith("enc:v2:next:");
        f.properties.getDecryptionKeys().remove("primary");
        assertThat(f.secrets.decrypt(merged)).isEqualTo("same-provider-key");
    }
}
