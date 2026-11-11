package com.hardmetal.export.shared.web;

import com.hardmetal.export.shared.exception.ErrorCode;

/**
 * 统一响应结构 {@code {code, msg, data, traceId}}。code = 0 表示成功。
 */
public record ApiResponse<T>(int code, String msg, T data, String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.OK.code(), ErrorCode.OK.defaultMsg(), data, TraceContext.current());
    }

    public static ApiResponse<Void> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String msg, T data) {
        return new ApiResponse<>(errorCode.code(), msg, data, TraceContext.current());
    }
}