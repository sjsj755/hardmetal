package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 原料基准价。字段名对齐《接口文档》12.6。
 *
 * @param material   原料类型，如 tungsten_powder
 * @param priceCnyKg 元/kg，字符串 4 位小数
 * @param date       基准日
 * @param source     数据来源
 * @param operator   录入人姓名；P0 未落 created_by，当前恒为 null
 */
public record MaterialPriceView(
        @Schema(description = "原料类型，如 tungsten_powder") String material,
        @Schema(description = "价格，元/kg，字符串") BigDecimal priceCnyKg,
        @Schema(description = "基准日") LocalDate date,
        @Schema(description = "数据来源") String source,
        @Schema(description = "录入人姓名；当前未实现，恒为 null") String operator) {
}