package com.hardmetal.export.auth;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 角色 → 权限点映射（《设计清单》H 权限矩阵的 P0 静态实现）。
 * <p>注意：这只是一份占位表，等 sys_role_perm 落地后由数据库驱动，
 * 并通过 Redis {@code perm:<uid>}（TTL 10min）提供，改角色即时生效。</p>
 */
public final class RolePerms {

    private static final Set<Perm> ADMIN_LIKE = EnumSet.allOf(Perm.class);

    private RolePerms() {
    }

    public static List<String> codes(String roleCode) {
        return of(roleCode).stream().map(Perm::code).toList();
    }

    public static Set<Perm> of(String roleCode) {
        if (roleCode == null) {
            return EnumSet.noneOf(Perm.class);
        }
        return switch (roleCode) {
            case "ADMIN", "BOSS" -> ADMIN_LIKE;
            case "SALES_MANAGER" -> EnumSet.of(
                    Perm.CUSTOMER_READ_ALL, Perm.CUSTOMER_WRITE, Perm.COST_READ,
                    Perm.QUOTE_CREATE, Perm.QUOTE_PRICE_OVERRIDE, Perm.QUOTE_APPROVE,
                    Perm.PRICE_LIST_WRITE, Perm.REFUND_READ);
            case "SALES" -> EnumSet.of(
                    Perm.CUSTOMER_READ_SELF, Perm.CUSTOMER_WRITE, Perm.QUOTE_CREATE);
            case "DOC_SPECIALIST" -> EnumSet.of(
                    Perm.CUSTOMER_READ_ALL, Perm.TARIFF_WRITE);
            case "FINANCE" -> EnumSet.of(
                    Perm.CUSTOMER_READ_ALL, Perm.COST_READ, Perm.REFUND_READ);
            default -> EnumSet.noneOf(Perm.class);
        };
    }
}