package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * 客户详情里的最近报价摘要。
 */
@Schema(description = "最近报价摘要")
public record RecentQuote(
        @Schema(description = "报价单 id") long id,
        @Schema(description = "报价单号", example = "QT-2026-0012") String quoteNo,
        @Schema(description = "报价状态，取字典 quote_status", example = "draft") String status,
        @Schema(description = "含税总金额，字符串，保留 2 位", example = "12500.00") BigDecimal totalAmount,
        @Schema(description = "币种", example = "EUR") String currency) {
}