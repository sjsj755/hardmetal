package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户列表项，字段名对齐《接口文档》3.1。
 * <p>{@code grossMarginAvg} 受字段级权限控制（cost:read），P0 未实现权限项时不出该字段。
 */
@Schema(description = "客户列表项")
public record CustomerView(
        @Schema(description = "客户 id", example = "1")
        long id,

        @Schema(description = "客户编码", example = "DE-0007")
        String code,

        @Schema(description = "中文名", example = "米勒硬质合金有限公司")
        String nameCn,

        @Schema(description = "英文名", example = "Hartmetall Müller GmbH")
        String nameEn,

        @Schema(description = "客户类型", example = "end_user")
        String customerType,

        @Schema(description = "等级 A/B/C/D", example = "A")
        String level,

        @Schema(description = "国家代码", example = "DE")
        String countryCode,

        @Schema(description = "加工材料")
        List<String> machiningMaterials,

        @Schema(description = "对标品牌")
        String benchmarkBrand,

        @Schema(description = "累计订单数")
        long orderCount,

        @Schema(description = "最近下单日期")
        LocalDate lastOrderAt) {
}