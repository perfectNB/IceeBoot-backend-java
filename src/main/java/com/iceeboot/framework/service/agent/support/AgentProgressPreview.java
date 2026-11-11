package com.iceeboot.framework.service.agent.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Pattern;

/** 过程只展示有限长度摘要，凭据字段在序列化前隐藏，文本中的常见令牌再次脱敏。 */
public final class AgentProgressPreview {
    public static final int MAX_CHARACTERS = 2000;
    private static final Pattern SECRET_KEY = Pattern.compile("(?i).*(password|passwd|secret|token|authorization|api.?key|credential|cookie|environment|private.?key).*");
    private static final Pattern ASSIGNMENT = Pattern.compile("(?i)((?:password|passwd|secret|token|authorization|api[_-]?key|credential|cookie)[\"']?\\s*[:=]\\s*)(?:\"[^\"]*\"|'[^']*'|[^\\s,;}]+)");
    private AgentProgressPreview() {}

    public static String input(Map<String, Object> input) {
        try { return text(new ObjectMapper().writeValueAsString(mask(input))); }
        catch (com.fasterxml.jackson.core.JsonProcessingException e) { return "参数摘要不可用"; }
    }

    private static Object mask(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), SECRET_KEY.matcher(String.valueOf(key)).matches() ? "[已隐藏]" : mask(item)));
            return result;
        }
        if (value instanceof List<?> list) return list.stream().map(AgentProgressPreview::mask).toList();
        return value;
    }

    public static String text(String value) { return text(value,MAX_CHARACTERS); }
    public static String text(String value,int maximum) {
        if (value == null) return "";
        String safe = ASSIGNMENT.matcher(value).replaceAll("$1[已隐藏]")
                .replaceAll("(?i)Bearer [a-z0-9._~+/=-]+", "Bearer [已隐藏]")
                .replaceAll("(?:sk-|e2b_)[A-Za-z0-9_-]{12,}", "[已隐藏]");
        return safe.length() <= maximum ? safe : safe.substring(0, maximum) + "\n…（结果摘要已截断）";
    }
}
