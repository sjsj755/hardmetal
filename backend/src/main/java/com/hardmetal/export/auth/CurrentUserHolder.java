package com.hardmetal.export.auth;

/**
 * 当前请求的登录用户。无登录态时返回 null。
 */
public final class CurrentUserHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private CurrentUserHolder() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new com.hardmetal.export.shared.exception.BizException(
                    com.hardmetal.export.shared.exception.ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}