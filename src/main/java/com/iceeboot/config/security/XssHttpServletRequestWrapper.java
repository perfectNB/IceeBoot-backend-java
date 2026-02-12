package com.iceeboot.config.security;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.web.util.HtmlUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * XSS请求包装器
 * 对请求参数和JSON请求体进行HTML转义
 *
 * @author CodeIcee
 * @since 2026-02-12
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

    private byte[] body;

    public XssHttpServletRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return value != null ? HtmlUtils.htmlEscape(value) : null;
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] escaped = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            escaped[i] = values[i] != null ? HtmlUtils.htmlEscape(values[i]) : null;
        }
        return escaped;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> escaped = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> entry : original.entrySet()) {
            String[] values = entry.getValue();
            if (values != null) {
                String[] escapedValues = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    escapedValues[i] = values[i] != null ? HtmlUtils.htmlEscape(values[i]) : null;
                }
                escaped.put(entry.getKey(), escapedValues);
            } else {
                escaped.put(entry.getKey(), null);
            }
        }
        return escaped;
    }

    @Override
    public String getHeader(String name) {
        String value = super.getHeader(name);
        return value != null ? HtmlUtils.htmlEscape(value) : null;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (body == null) {
            body = readBody();
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                // no-op
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    /**
     * 读取并转义请求体中的HTML特殊字符
     * 仅对字符串值进行转义，保留JSON结构
     */
    private byte[] readBody() throws IOException {
        byte[] original = super.getInputStream().readAllBytes();
        if (original.length == 0) {
            return original;
        }
        String content = new String(original, StandardCharsets.UTF_8);
        String contentType = super.getContentType();
        // 仅对JSON请求体进行XSS过滤
        if (contentType != null && contentType.contains("application/json")) {
            content = escapeJsonStringValues(content);
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 转义JSON中字符串值的HTML特殊字符
     * 简单实现：对JSON字符串值中的 < > " ' & 进行转义
     */
    private String escapeJsonStringValues(String json) {
        StringBuilder result = new StringBuilder(json.length());
        boolean inString = false;
        boolean isKey = true;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && inString) {
                result.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                result.append(c);
                if (inString) {
                    inString = false;
                } else {
                    inString = true;
                }
                continue;
            }

            if (c == ':' && !inString) {
                isKey = false;
                result.append(c);
                continue;
            }

            if ((c == ',' || c == '}' || c == ']') && !inString) {
                isKey = true;
                result.append(c);
                continue;
            }

            // 仅对值（非键）中的字符串进行HTML转义
            if (inString && !isKey) {
                switch (c) {
                    case '<' -> result.append("&lt;");
                    case '>' -> result.append("&gt;");
                    default -> result.append(c);
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
