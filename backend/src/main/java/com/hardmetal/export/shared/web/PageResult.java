package com.hardmetal.export.shared.web;

import java.util.List;

/**
 * 统一分页结果。《接口文档》0.4：page 从 1 开始，size 默认 20、上限 200。
 * <p>data 形如 {@code {"items":[],"total":137,"page":1,"size":20}}。
 */
public record PageResult<T>(List<T> items, long total, int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 200;

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        return new PageResult<>(items, total, normalizePage(page), normalizeSize(size));
    }

    public static int normalizePage(int page) {
        return page < 1 ? 1 : page;
    }

    public static int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public static int offset(int page, int size) {
        return (normalizePage(page) - 1) * normalizeSize(size);
    }
}