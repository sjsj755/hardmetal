package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 客户价格表摘要：生效中的价格条数、30 天内即将过期的条数。
 */
@Schema(description = "客户价格表摘要")
public record PriceSummary(
        @Schema(description = "生效中的价格条数", example = "12") int activeItems,
        @Schema(description = "30 天内即将过期的条数", example = "2") int expiringSoon) {
}