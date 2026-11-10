package com.iceeboot.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.util.Map;

public final class SchemaSupport {
  private static final ObjectMapper JSON = new ObjectMapper();

  private SchemaSupport() {}

  public static void validate(Map<String, Object> schema, Object value) {
    var compiled =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(JSON.valueToTree(schema));
    if (!compiled.validate(JSON.valueToTree(value)).isEmpty())
      throw new IllegalArgumentException("内容不符合配置的 JSON Schema");
  }

  public static void checkSchema(Map<String, Object> schema) {
    if (schema != null) {
      rejectRemoteReferences(schema);
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
          .getSchema(JSON.valueToTree(schema));
    }
  }

  private static void rejectRemoteReferences(Object value) {
    if (value instanceof Map<?, ?> map) {
      for (var e : map.entrySet()) {
        if (("$ref".equals(e.getKey()) || "$dynamicRef".equals(e.getKey()))
            && (!(e.getValue() instanceof String s) || !s.startsWith("#")))
          throw new IllegalArgumentException("JSON Schema 只允许内部引用");
        rejectRemoteReferences(e.getValue());
      }
    } else if (value instanceof Iterable<?> list)
      for (Object item : list) rejectRemoteReferences(item);
  }
}
