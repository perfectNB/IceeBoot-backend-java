package com.iceeboot.common.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.iceeboot.common.annotation.Sensitive;

import java.io.IOException;

/**
 * 敏感字段脱敏序列化器
 * 配合 @Sensitive 注解使用，序列化时自动对字段值进行脱敏
 *
 * @author CodeIcee
 * @date 2025-08-11
 */
public class SensitiveSerializer extends JsonSerializer<String>
        implements ContextualSerializer {

    private int prefixKeep;
    private int suffixKeep;

    public SensitiveSerializer() {
    }

    public SensitiveSerializer(int prefixKeep, int suffixKeep) {
        this.prefixKeep = prefixKeep;
        this.suffixKeep = suffixKeep;
    }

    @Override
    public void serialize(String value, JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {
        gen.writeString(DesensitizeUtils.desensitizeCustom(
                value, prefixKeep, suffixKeep));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov,
                                               BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return prov.findNullValueSerializer(null);
        }
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (annotation != null) {
            return new SensitiveSerializer(
                    annotation.prefixKeep(), annotation.suffixKeep());
        }
        return prov.findValueSerializer(property.getType(), property);
    }
}
