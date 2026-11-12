package com.hardmetal.export.shared.web;

import java.util.List;

/**
 * 不分页列表的统一包装：{@code data: {items:[...]}}。
 * <p>用于港口、原料价等契约未定义分页的列表接口。
 */
public record ItemsResult<T>(List<T> items) {

    public static <T> ItemsResult<T> of(List<T> items) {
        return new ItemsResult<>(items);
    }
}