package com.hardmetal.export.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码。《接口文档》0.6 错误码表一一对应，新增错误码必须先改文档与契约。
 */
public enum ErrorCode {

    OK(0, HttpStatus.OK, "ok"),

    PARAM_INVALID(40001, HttpStatus.BAD_REQUEST, "参数校验失败"),
    FILE_INVALID(40002, HttpStatus.BAD_REQUEST, "文件格式或大小不符"),

    UNAUTHORIZED(40101, HttpStatus.UNAUTHORIZED, "未登录或登录已失效"),
    TOKEN_EXPIRED(40102, HttpStatus.UNAUTHORIZED, "登录已过期，请刷新令牌"),
    BAD_CREDENTIALS(40103, HttpStatus.UNAUTHORIZED, "账号或密码错误"),

    FORBIDDEN(40301, HttpStatus.FORBIDDEN, "无操作权限"),

    NOT_FOUND(40401, HttpStatus.NOT_FOUND, "资源不存在或已删除"),
    PRICE_MISSING(40403, HttpStatus.NOT_FOUND, "取不到有效价，请先维护价格表"),
    FREIGHT_MISSING(40404, HttpStatus.NOT_FOUND, "该航线无可用运价，请手工询价"),

    STATE_CONFLICT(40901, HttpStatus.CONFLICT, "当前状态不允许该操作"),
    PRICE_AMBIGUOUS(40902, HttpStatus.CONFLICT, "同优先级存在多条有效价格"),
    DUPLICATE_KEY(40903, HttpStatus.CONFLICT, "编码或唯一键重复"),
    UNCLOSED_BUSINESS(40904, HttpStatus.CONFLICT, "存在未结业务，不可删除"),
    DUPLICATE_CONFIG(40905, HttpStatus.CONFLICT, "同 key 同生效日重复配置"),
    DUPLICATE_SUBMIT(40906, HttpStatus.CONFLICT, "重复提交"),

    RULE_VIOLATION(42201, HttpStatus.UNPROCESSABLE_ENTITY, "业务规则不通过"),
    COMPLIANCE_BLOCKED(42301, HttpStatus.LOCKED, "被合规规则阻断"),

    INTERNAL_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "服务异常"),
    DATA_INCONSISTENT(50002, HttpStatus.INTERNAL_SERVER_ERROR, "数据一致性错误");

    private final int code;
    private final HttpStatus status;
    private final String defaultMsg;

    ErrorCode(int code, HttpStatus status, String defaultMsg) {
        this.code = code;
        this.status = status;
        this.defaultMsg = defaultMsg;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMsg() {
        return defaultMsg;
    }
}