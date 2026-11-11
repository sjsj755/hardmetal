package com.hardmetal.export.shared.exception;

/**
 * 业务异常。只表达"可预期的业务拒绝"，不用于流程控制之外的场景。
 * <p>data 原样返回给前端，例如 {@code {"rule":"R-Q05","current":"-3.2","threshold":"0"}}。
 */
public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object data;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMsg(), null);
    }

    public BizException(ErrorCode errorCode, String msg) {
        this(errorCode, msg, null);
    }

    public BizException(ErrorCode errorCode, String msg, Object data) {
        super(msg);
        this.errorCode = errorCode;
        this.data = data;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Object data() {
        return data;
    }
}