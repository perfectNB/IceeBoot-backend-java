package com.iceeboot.framework.agent;

import static org.assertj.core.api.Assertions.*;

import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.iceeboot.framework.enums.agent.*;
import com.iceeboot.framework.model.agent.*;
import com.iceeboot.framework.service.agent.runtime.SchemaSupport;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class AgentSchemaValidationTest {
    @Test
    void invalidSchemaIsRejectedBeforeAnyToolRecordIsWritten() throws Exception {
        var f = new AgentFixture();
        for (Map<String, Object> schema :
                List.<Map<String, Object>>of(
                        Map.of(
                                "type",
                                "object",
                                "properties",
                                Map.of("x", Map.of("$ref", "#/missing"))),
                        Map.of("type", "object", "properties", Map.of("x", Map.of("type", "BAD"))),
                        Map.of("type", "object", "required", "not-an-array"),
                        Map.of(
                                "type",
                                "object",
                                "properties",
                                Map.of("x", Map.of("minimum", "not-a-number"))))) {
            assertThatThrownBy(
                            () ->
                                    f.configs.saveCapability(
                                            new Capability(
                                                    null,
                                                    CapabilityKind.TOOL,
                                                    "invalid",
                                                    "test",
                                                    "",
                                                    true,
                                                    null,
                                                    new ToolConfig(
                                                            ToolKind.HTTP,
                                                            "https://example.invalid",
                                                            "POST",
                                                            Map.of(),
                                                            schema,
                                                            null,
                                                            false,
                                                            null),
                                                    null,
                                                    null)))
                    .isInstanceOf(IceeBootException.class)
                    .hasMessage(ResponseMessage.Agent.SCHEMA_INVALID);
            assertThat(
                            f.jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM t_agent_capability", Integer.class))
                    .isZero();
            assertThat(f.jdbc.queryForObject("SELECT COUNT(*) FROM t_agent_tool", Integer.class))
                    .isZero();
        }
    }

    @Test
    void agentOutputSchemaIsRejectedDuringSaveRatherThanAtModelExecution() throws Exception {
        var f = new AgentFixture();
        var model = f.model();
        var draft =
                f.draft(
                        "invalid_output",
                        model.id(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        false,
                        null);
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) f.json.valueToTree(draft);
        node.put("outputFormat", "JSON_SCHEMA");
        node.set(
                "outputSchema",
                f.json.valueToTree(
                        Map.of(
                                "type",
                                "object",
                                "properties",
                                Map.of("x", Map.of("$ref", "#/missing")))));
        AgentSpec invalid = f.json.treeToValue(node, AgentSpec.class);
        assertThatThrownBy(() -> f.configs.saveAgent(invalid))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_INVALID);
        assertThat(f.store.agents()).isEmpty();
    }

    @Test
    void validInternalReferencesRecursiveSchemasAndConstraintsAreAccepted() {
        SchemaSupport.checkSchema(
                Map.of(
                        "type",
                        "object",
                        "$defs",
                        Map.of("value", Map.of("type", "string")),
                        "properties",
                        Map.of("x", Map.of("$ref", "#/$defs/value")),
                        "required",
                        List.of("x")));
        SchemaSupport.checkSchema(
                Map.of("type", "object", "properties", Map.of("child", Map.of("$ref", "#"))));
        SchemaSupport.checkSchema(Map.of("type", "object", "properties", Map.of("blocked", false)));
        SchemaSupport.checkSchema(
                Map.of("$schema", "http://json-schema.org/draft-07/schema#", "type", "object"));
        SchemaSupport.checkSchema(null);
        assertThatThrownBy(
                        () ->
                                SchemaSupport.validate(
                                        Map.of("type", "object", "required", List.of("x")),
                                        Map.of()))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_CONTENT_MISMATCH);
    }

    @Test
    void unsupportedDialectAndRemoteReferencesAreRejectedWithoutFetchingThem() {
        assertThatThrownBy(
                        () ->
                                SchemaSupport.checkSchema(
                                        Map.of(
                                                "$schema",
                                                "https://example.invalid/schema",
                                                "type",
                                                "object")))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_INVALID);
        assertThatThrownBy(
                        () ->
                                SchemaSupport.checkSchema(
                                        Map.of("$ref", "https://example.invalid/schema")))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_LOCAL_REFERENCES_ONLY);
    }

    @Test
    void publishingCannotBypassValidationForPreviouslyStoredInvalidTool() throws Exception {
        var f = new AgentFixture();
        var model = f.model();
        long tool =
                f.store.saveCapability(
                        new Capability(
                                null,
                                CapabilityKind.TOOL,
                                "old_invalid",
                                "test",
                                "",
                                true,
                                null,
                                new ToolConfig(
                                        ToolKind.HTTP,
                                        "https://example.invalid",
                                        "POST",
                                        Map.of(),
                                        Map.of(
                                                "type",
                                                "object",
                                                "properties",
                                                Map.of("x", Map.of("$ref", "#/missing"))),
                                        null,
                                        false,
                                        null),
                                null,
                                null));
        var agent =
                f.save(
                        "legacy",
                        model.id(),
                        List.of(),
                        List.of(tool),
                        List.of(),
                        List.of(),
                        false,
                        null);
        assertThatThrownBy(() -> f.publish(agent))
                .isInstanceOf(IceeBootException.class)
                .hasMessage(ResponseMessage.Agent.SCHEMA_INVALID);
        assertThat(f.store.releases(agent.draft().id())).isEmpty();
        assertThat(f.store.agent(agent.draft().id()).activeReleaseId()).isEmpty();
    }
}
