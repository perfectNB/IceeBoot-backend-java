package com.iceeboot.framework.service.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceeboot.common.constant.ResponseMessage;
import com.iceeboot.common.exception.IceeBootException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;

import java.util.Map;

public final class SchemaSupport {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SchemaRegistry REGISTRY =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12);

    private SchemaSupport() {}

    public static void validate(Map<String, Object> schema, Object value) {
        try {
            rejectRemoteReferences(schema);
            var compiled =
                    SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
                            .getSchema(JSON.valueToTree(schema));
            if (!compiled.validate(JSON.valueToTree(value)).isEmpty())
                throw new IceeBootException(ResponseMessage.Agent.SCHEMA_CONTENT_MISMATCH);
        } catch (IceeBootException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IceeBootException(ResponseMessage.Agent.SCHEMA_INVALID, e);
        }
    }

    public static void checkSchema(Map<String, Object> schema) {
        if (schema == null) return;
        try {
            rejectRemoteReferences(schema);
            var schemaNode = JSON.valueToTree(schema);
            SpecificationVersion version = SpecificationVersion.DRAFT_2020_12;
            if (schema.containsKey("$schema")) {
                Object declared = schema.get("$schema");
                if (!(declared instanceof String dialect))
                    throw new IceeBootException(ResponseMessage.Agent.SCHEMA_INVALID);
                String normalized = normalizeDialect(dialect);
                version =
                        java.util.Arrays.stream(SpecificationVersion.values())
                                .filter(v -> normalizeDialect(v.getDialectId()).equals(normalized))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IceeBootException(
                                                        ResponseMessage.Agent.SCHEMA_INVALID));
            }
            // 元模式验证关键字的类型与约束，预加载会解析未触发分支中的内部引用。
            var metaSchema = REGISTRY.getSchema(SchemaLocation.of(version.getDialectId()));
            if (!metaSchema.validate(schemaNode).isEmpty())
                throw new IceeBootException(ResponseMessage.Agent.SCHEMA_INVALID);
            SchemaRegistry.withDefaultDialect(version).getSchema(schemaNode).initializeValidators();
        } catch (IceeBootException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IceeBootException(ResponseMessage.Agent.SCHEMA_INVALID, e);
        }
    }

    private static String normalizeDialect(String dialect) {
        String normalized =
                dialect.endsWith("#") ? dialect.substring(0, dialect.length() - 1) : dialect;
        return normalized.replaceFirst("^http://", "https://");
    }

    private static void rejectRemoteReferences(Object value) {
        if (value instanceof Map<?, ?> map) {
            for (var e : map.entrySet()) {
                if (("$ref".equals(e.getKey()) || "$dynamicRef".equals(e.getKey()))
                        && (!(e.getValue() instanceof String s) || !s.startsWith("#")))
                    throw new IceeBootException(ResponseMessage.Agent.SCHEMA_LOCAL_REFERENCES_ONLY);
                rejectRemoteReferences(e.getValue());
            }
        } else if (value instanceof Iterable<?> list)
            for (Object item : list) rejectRemoteReferences(item);
    }
}
