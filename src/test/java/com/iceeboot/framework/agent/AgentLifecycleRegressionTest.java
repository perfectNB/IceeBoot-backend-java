package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.mapper.agent.AgentDefinitionMapper;
import com.iceeboot.framework.model.agent.*;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.*;

class AgentLifecycleRegressionTest {
    @Test
    void concurrentDraftSaveCannotRevertFirstOrLaterPublication() throws Exception {
        for (boolean alreadyPublished : List.of(false, true)) {
            var f = new AgentFixture();
            var model = f.model();
            var agent =
                    f.save(
                            "analyst",
                            model.id(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            false,
                            null);
            if (alreadyPublished) f.publish(agent);
            ObjectNode editedNode = f.json.valueToTree(agent.draft());
            editedNode.put("name", "edited draft");
            AgentSpec edited = f.json.treeToValue(editedNode, AgentSpec.class);
            Object target = AopTestUtils.getTargetObject(f.store);
            var mapper = (AgentDefinitionMapper) ReflectionTestUtils.getField(target, "agents");
            var readDone = new CountDownLatch(1);
            var resume = new CountDownLatch(1);
            var wrapped =
                    (AgentDefinitionMapper)
                            Proxy.newProxyInstance(
                                    AgentDefinitionMapper.class.getClassLoader(),
                                    new Class<?>[] {AgentDefinitionMapper.class},
                                    (proxy, method, arguments) -> {
                                        try {
                                            Object result = method.invoke(mapper, arguments);
                                            if (method.getName().equals("selectById")
                                                    && Thread.currentThread()
                                                            .getName()
                                                            .equals("draft-save")) {
                                                readDone.countDown();
                                                if (!resume.await(10, TimeUnit.SECONDS))
                                                    throw new AssertionError(
                                                            "draft save timed out");
                                            }
                                            return result;
                                        } catch (InvocationTargetException e) {
                                            throw e.getCause();
                                        }
                                    });
            ReflectionTestUtils.setField(target, "agents", wrapped);
            var executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "draft-save"));
            try {
                var saving = executor.submit(() -> f.configs.saveAgent(edited));
                assertThat(readDone.await(10, TimeUnit.SECONDS)).isTrue();
                var released = f.publish(agent);
                resume.countDown();
                saving.get(10, TimeUnit.SECONDS);
                var current = f.store.agent(agent.draft().id());
                assertThat(current.activeReleaseId()).isEqualTo(released.releaseId());
                assertThat(current.activeVersion()).isEqualTo(alreadyPublished ? 2 : 1);
                assertThat(current.draft().name()).isEqualTo("edited draft");
            } finally {
                resume.countDown();
                executor.shutdownNow();
                ReflectionTestUtils.setField(target, "agents", mapper);
            }
        }
    }

    @Test
    void resourceCodeCanBeReusedRepeatedlyAfterLogicalDeletionButNotWhileActive() throws Exception {
        var f = new AgentFixture();
        Long previousId = null;
        for (int i = 0; i < 3; i++) {
            var tool = f.http("reusable", "https://example.invalid/data");
            assertThat(tool.id()).isNotEqualTo(previousId);
            assertThatThrownBy(() -> f.http("reusable", "https://example.invalid/data"))
                    .isInstanceOf(DuplicateKeyException.class);
            f.store.deleteCapability(tool.id());
            previousId = tool.id();
        }
        assertThat(
                        f.jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_agent_capability WHERE code='reusable' AND"
                                        + " del_flag=1",
                                Integer.class))
                .isEqualTo(3);
        assertThat(
                        f.jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_agent_tool WHERE code='reusable' AND"
                                        + " del_flag=1",
                                Integer.class))
                .isEqualTo(3);
        assertThat(f.store.capabilities()).isEmpty();
    }

    @Test
    void agentCodeCanBeReusedWhileArchivedSnapshotsStayReadable() throws Exception {
        var f = new AgentFixture();
        var model = f.model();
        for (int i = 0; i < 3; i++) {
            var agent =
                    f.save(
                            "reusable_agent",
                            model.id(),
                            List.of(),
                            List.of(),
                            List.of(),
                            List.of(),
                            false,
                            null);
            var released = f.publish(agent);
            assertThatThrownBy(
                            () ->
                                    f.save(
                                            "reusable_agent",
                                            model.id(),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            List.of(),
                                            false,
                                            null))
                    .isInstanceOf(DuplicateKeyException.class);
            f.store.deleteAgent(agent.draft().id());
            assertThatThrownBy(() -> f.store.agentByCode("reusable_agent"))
                    .isInstanceOf(IceeBootException.class);
            assertThat(f.store.release(released.releaseId()).agent().id())
                    .isEqualTo(agent.draft().id());
        }
        assertThat(
                        f.jdbc.queryForObject(
                                "SELECT COUNT(*) FROM t_agent_definition WHERE"
                                        + " code='reusable_agent' AND del_flag=1",
                                Integer.class))
                .isEqualTo(3);
    }
}
