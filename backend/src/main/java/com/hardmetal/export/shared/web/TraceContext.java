package com.hardmetal.export.shared.web;

/**
 * 当前请求的 traceId 上下文，由 {@link TraceIdFilter} 写入并在请求结束时清理。
 */
public final class TraceContext {

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private TraceContext() {
    }

    public static void set(String traceId) {
        HOLDER.set(traceId);
    }

    public static String current() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}