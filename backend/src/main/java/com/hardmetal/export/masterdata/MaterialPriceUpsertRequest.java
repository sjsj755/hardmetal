package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 录入原料基准价。《接口文档》12.7：手工兜底，来源缺省 manual。
 */
public record MaterialPriceUpsertRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,31}$", message = "原料类型编码非法")
        @Schema(description = "原料类型", example = "tungsten_powder")
        String material,

        @NotNull
        @DecimalMin(value = "0.0001", message = "原料价必须大于 0")
        @Schema(description = "价格，元/kg", example = "391000.00")
        BigDecimal priceCnyKg,

        @NotNull
        @Schema(description = "基准日", example = "2026-11-13")
        LocalDate date,

        @Pattern(regexp = "^(auto|manual)$", message = "source 只允许 auto 或 manual")
        @Schema(description = "来源，缺省 manual", example = "manual")
        String source) {
}