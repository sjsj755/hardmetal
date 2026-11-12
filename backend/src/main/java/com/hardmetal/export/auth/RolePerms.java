package com.hardmetal.export.auth;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 角色 → 权限点映射（《设计清单》H 权限矩阵的 P0 静态实现）。
 * <p>V4 迁移后系统只有两级角色：{@code ADMIN}（全量）与 {@code SALES}
 * （本人客户范围 + 客户写入 + 新建报价）。历史角色代码（BOSS / SALES_MANAGER /
 * DOC_SPECIALIST / FINANCE）不再映射，返回空集；若仍有旧角色用户，登录后
 * 没有任何权限，属有意的收口行为。</p>
 * <p>注意：这仍是一份占位表，等 sys_role_perm 落地后由数据库驱动，
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
            case "ADMIN" -> ADMIN_LIKE;
            case "SALES" -> EnumSet.of(
                    Perm.CUSTOMER_READ_SELF, Perm.CUSTOMER_WRITE, Perm.QUOTE_CREATE);
            default -> EnumSet.noneOf(Perm.class);
        };
    }
}