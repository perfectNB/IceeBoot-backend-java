/*
 * @Description: 请求链路追踪过滤器
 * @Date: 2025-08-11
 * @Author: CodeIcee
 */
package com.iceeboot.config.trace;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器
 * <p>
 * 为每个请求生成唯一的 traceId，存入 SLF4J MDC 中，
 * 并通过响应头 X-Trace-Id 返回给调用方。
 * 如果请求头中已携带 X-Trace-Id，则沿用该值（支持分布式链路透传）。
 *
 * @author CodeIcee
 * @since 2026-02-12
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TraceFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_MDC_KEY = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // 优先使用请求头中传入的 traceId（分布式透传）
            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (!StringUtils.hasText(traceId)) {
                traceId = UUID.randomUUID().toString().replace("-", "");
            }

            // 放入 MDC
            MDC.put(TRACE_ID_MDC_KEY, traceId);

            // 设置响应头
            httpResponse.setHeader(TRACE_ID_HEADER, traceId);

            chain.doFilter(request, response);
        } finally {
            // 清理 MDC，防止线程复用导致数据污染
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
