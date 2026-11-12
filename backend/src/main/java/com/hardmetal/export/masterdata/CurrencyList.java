package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 汇率列表。字段名对齐《接口文档》12.5：{@code {date, base, items}}。
 */
public record CurrencyList(
        @Schema(description = "汇率日期，缺省为库中最新一天") LocalDate date,
        @Schema(description = "基准币种；当前仅支持 CNY") String base,
        @Schema(description = "各币种对 CNY 的汇率") List<CurrencyView> items) {
}