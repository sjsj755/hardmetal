package com.hardmetal.export.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * 为每个请求生成 traceId：写入 MDC（日志）、响应头 X-Trace-Id，并放入 {@link TraceContext}。
 * <p>优先级最高，保证后续过滤器与异常处理都能拿到 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_KEY = "traceId";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolve(request);
        TraceContext.set(traceId);
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
            TraceContext.clear();
        }
    }

    private String resolve(HttpServletRequest request) {
        String incoming = request.getHeader(HEADER);
        if (incoming != null && incoming.length() >= 8 && incoming.length() <= 64
                && incoming.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return incoming;
        }
        char[] buf = new char[8];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = HEX[RANDOM.nextInt(HEX.length)];
        }
        return new String(buf);
    }
}