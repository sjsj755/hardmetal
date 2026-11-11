package com.hardmetal.export.auth;

/**
 * 权限点（对应《设计清单》H 权限矩阵与技术设计 2.3.2）。
 * P0 阶段先按角色静态映射；后续接 sys_role 权限表 + Redis {@code perm:<uid>}，
 * 届时本枚举只保留常量定义。
 */
public enum Perm {

    CUSTOMER_READ_SELF("customer:read:self"),
    CUSTOMER_READ_ALL("customer:read:all"),
    CUSTOMER_WRITE("customer:write"),
    /** 成本/毛利可见 —— 铁律：业务员不得看到。 */
    COST_READ("cost:read"),
    QUOTE_CREATE("quote:create"),
    QUOTE_PRICE_OVERRIDE("quote:price:override"),
    QUOTE_APPROVE("quote:approve"),
    PRICE_LIST_WRITE("price:write"),
    TARIFF_WRITE("tariff:write"),
    CONTROL_RULE_WRITE("control:write"),
    CONTROL_RELEASE("control:release"),
    REFUND_READ("refund:read");

    private final String code;

    Perm(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}