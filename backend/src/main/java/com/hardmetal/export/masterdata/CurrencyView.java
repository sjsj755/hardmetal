package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 单一币种汇率。{@code mid/buy/sell} 均为“1 单位外币 = N CNY”，由 BigDecimal 序列化成字符串。
 */
public record CurrencyView(
        @Schema(description = "币种代码，如 USD") String currency,
        @Schema(description = "中间价，字符串 6 位小数") BigDecimal mid,
        @Schema(description = "买入价") BigDecimal buy,
        @Schema(description = "卖出价") BigDecimal sell,
        @Schema(description = "数据来源，如 BOC") String source) {
}