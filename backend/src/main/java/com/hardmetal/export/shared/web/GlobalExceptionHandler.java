package com.hardmetal.export.shared.web;

import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理：把任何异常收敛成统一响应 {@code {code,msg,data,traceId}}。
 * <p>前端只看 code，不看 HTTP 状态码；HTTP 状态仍按错误码语义对齐，方便网关与监控。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Object>> handleBiz(BizException ex) {
        ErrorCode ec = ex.errorCode();
        return ResponseEntity.status(ec.status()).body(ApiResponse.fail(ec, ex.getMessage(), ex.data()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ApiResponse<Object>> handleBinding(Exception ex) {
        FieldError first = null;
        if (ex instanceof MethodArgumentNotValidException manve && manve.getBindingResult().hasFieldErrors()) {
            first = manve.getBindingResult().getFieldErrors().getFirst();
        } else if (ex instanceof BindException be && be.getBindingResult().hasFieldErrors()) {
            first = be.getBindingResult().getFieldErrors().getFirst();
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        if (first != null) {
            detail.put("field", first.getField());
            detail.put("reason", first.getDefaultMessage());
        }
        String msg = first == null ? ErrorCode.PARAM_INVALID.defaultMsg()
                : "参数校验失败：" + first.getField() + " " + first.getDefaultMessage();
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.status())
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, msg, detail));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraint(ConstraintViolationException ex) {
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.status())
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, "参数校验失败：" + ex.getMessage(), null));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(Exception ex) {
        return ResponseEntity.status(ErrorCode.PARAM_INVALID.status())
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, "参数校验失败：" + ex.getMessage(), null));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.status())
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND, "资源不存在：" + ex.getResourcePath(), null));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID, "请求方法不支持：" + ex.getMethod(), null));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        // 唯一约束兜底：业务层已先查重，命中这里说明并发撞车或漏查，返回 40903 而不是 500。
        log.warn("数据完整性冲突 traceId={}", TraceContext.current(), ex);
        return ResponseEntity.status(ErrorCode.DUPLICATE_KEY.status())
                .body(ApiResponse.fail(ErrorCode.DUPLICATE_KEY, "编码或唯一键重复", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception ex) {
        log.error("未处理异常 traceId={}", TraceContext.current(), ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.status())
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMsg(), null));
    }
}