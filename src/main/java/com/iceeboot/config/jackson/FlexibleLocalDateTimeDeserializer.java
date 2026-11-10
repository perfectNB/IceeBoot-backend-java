package com.iceeboot.config.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 兼容前后端常见的 LocalDateTime 字符串格式，避免因分隔符不同导致请求直接失败。
 */
public class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    @Override
    public Class<?> handledType() {
        return LocalDateTime.class;
    }

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String text = parser.getValueAsString();
        if (text == null) {
            return null;
        }

        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }

        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一种格式
            }
        }

        throw JsonMappingException.from(parser,
                "无法解析日期时间，支持格式：yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd'T'HH:mm:ss，实际值: " + value);
    }
}
