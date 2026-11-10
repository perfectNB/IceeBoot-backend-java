package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.runtime.SchemaSupport;
import com.iceeboot.framework.service.agent.security.AgentSecrets;

import org.junit.jupiter.api.*;

import java.util.*;

class AgentConfigurationTest {
    AgentFixture f;

    @BeforeEach
    void setup() throws Exception {
        f = new AgentFixture();
    }

    @Test
    void encryptsSecretsAndPreservesMaskedEdits() {
        var m = f.model();
        assertThat(m.apiKey()).isEqualTo(AgentSecrets.MASK);
        var raw = f.store.model(m.id());
        assertThat(raw.apiKey()).startsWith("enc:v1:").doesNotContain("test-key");
        assertThat(f.secrets.decrypt(raw.apiKey())).isEqualTo("test-key");
        f.configs.saveModel(m);
        assertThat(f.secrets.decrypt(f.store.model(m.id()).apiKey())).isEqualTo("test-key");
    }

    @Test
    void rejectsMissingMasterKeyAndTamperedCiphertext() {
        f.properties.setEncryptionKey("");
        assertThatThrownBy(() -> f.secrets.encrypt("secret"))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        f.properties.setEncryptionKey(Base64.getEncoder().encodeToString(new byte[32]));
        String encrypted = f.secrets.encrypt("secret");
        assertThatThrownBy(
                        () ->
                                f.secrets.decrypt(
                                        encrypted.substring(0, encrypted.length() - 4) + "AAAA"))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
    }

    @Test
    void releaseFreezesModelSkillResourceAndChildVersions() {
        var m = f.model();
        var child =
                f.save("child", m.id(), List.of(), List.of(), List.of(), List.of(), false, null);
        var c1 = f.publish(child);
        var skill =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.SKILL,
                                "analyze",
                                "分析",
                                "分析说明",
                                true,
                                new SkillConfig(
                                        "version-one",
                                        Map.of("scripts/run.py", "print(1)"),
                                        List.of()),
                                null,
                                null,
                                null));
        var parent =
                f.save(
                        "parent",
                        m.id(),
                        List.of(skill.id()),
                        List.of(),
                        List.of(),
                        List.of(child.draft().id()),
                        false,
                        null);
        var p1 = f.publish(parent);
        f.configs.saveCapability(
                new Capability(
                        skill.id(),
                        skill.kind(),
                        skill.code(),
                        skill.name(),
                        skill.description(),
                        true,
                        new SkillConfig(
                                "version-two", Map.of("scripts/run.py", "print(2)"), List.of()),
                        null,
                        null,
                        null));
        var c2 = f.publish(child);
        var p2 = f.publish(parent);
        assertThat(c2.version()).isEqualTo(2);
        assertThat(p1.children().getFirst().releaseId()).isEqualTo(c1.releaseId());
        assertThat(p2.children().getFirst().releaseId()).isEqualTo(c2.releaseId());
        assertThat(f.store.release(p1.releaseId()).capabilities().getFirst().skill().resources())
                .containsEntry("scripts/run.py", "print(1)");
        assertThat(f.store.json(p1)).doesNotContain("test-key");
    }

    @Test
    void agentCodeCannotChange() {
        var a =
                f.save(
                        "fixed",
                        f.model().id(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        null);
        var other =
                com.iceeboot.framework.service.agent.convert.AgentModelConverter.withId(
                        f.draft(
                                "changed",
                                a.draft().modelConfigId(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                false,
                                null),
                        a.draft().id());
        assertThatThrownBy(() -> f.configs.saveAgent(other)).hasMessageContaining("不可修改");
    }

    @Test
    void preventsCyclesAcrossPublishedChildGraphs() {
        var m = f.model();
        var a = f.save("a", m.id(), List.of(), List.of(), List.of(), List.of(), false, null);
        f.publish(a);
        var b =
                f.save(
                        "b",
                        m.id(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(a.draft().id()),
                        false,
                        null);
        f.publish(b);
        var changed =
                com.iceeboot.framework.service.agent.convert.AgentModelConverter.withId(
                        f.draft(
                                "a",
                                m.id(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(b.draft().id()),
                                false,
                                null),
                        a.draft().id());
        f.configs.saveAgent(changed);
        assertThatThrownBy(() -> f.configs.publish(a.draft().id(), "1")).hasMessageContaining("循环");
    }

    @Test
    void scriptToolCannotPublishWithoutE2b() {
        var script =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.TOOL,
                                "script",
                                "脚本",
                                "运行脚本",
                                true,
                                null,
                                new ToolConfig(
                                        ToolKind.SCRIPT,
                                        null,
                                        null,
                                        Map.of(),
                                        Map.of("type", "object"),
                                        "python3 run.py",
                                        false,
                                        null),
                                null,
                                null));
        var a =
                f.save(
                        "a",
                        f.model().id(),
                        List.of(),
                        List.of(script.id()),
                        List.of(),
                        List.of(),
                        false,
                        null);
        assertThatThrownBy(() -> f.publish(a)).hasMessageContaining("E2B");
    }

    @Test
    void rejectsUnsafeResourceAndRemoteSchemaReferences() {
        for (String path : List.of("../secret", "/tmp/a", "scripts/../../a", "scripts\\a"))
            assertThatThrownBy(
                            () ->
                                    com.iceeboot.framework.service.agent.impl
                                            .AgentConfigurationServiceImpl.resourcePath(path))
                    .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        assertThatThrownBy(
                        () ->
                                SchemaSupport.checkSchema(
                                        Map.of("$ref", "https://example.invalid/schema")))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
    }

    @Test
    void rejectsWrongSchemaAndWrongBoundCapabilityKind() {
        assertThatThrownBy(
                        () ->
                                SchemaSupport.validate(
                                        Map.of("type", "object", "required", List.of("answer")),
                                        Map.of()))
                .isInstanceOf(com.iceeboot.common.exception.IceeBootException.class);
        var m = f.model();
        var tool = f.http("lookup", "https://example.invalid/data");
        var a =
                f.save(
                        "a",
                        m.id(),
                        List.of(tool.id()),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        null);
        assertThatThrownBy(() -> f.publish(a)).hasMessageContaining("类型");
    }

    @Test
    void skillToolsAreFrozenIntoRelease() {
        var m = f.model();
        var tool = f.http("lookup", "https://example.invalid/data");
        var skill =
                f.configs.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.SKILL,
                                "s",
                                "技能",
                                "技能说明",
                                true,
                                new SkillConfig("使用 lookup", Map.of(), List.of(tool.id())),
                                null,
                                null,
                                null));
        var p =
                f.publish(
                        f.save(
                                "a",
                                m.id(),
                                List.of(skill.id()),
                                List.of(),
                                List.of(),
                                List.of(),
                                false,
                                null));
        assertThat(p.capabilities()).extracting(Capability::code).containsExactly("s", "lookup");
    }
}
