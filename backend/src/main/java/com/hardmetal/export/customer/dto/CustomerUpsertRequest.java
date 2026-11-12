package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新建/更新客户入参，字段名对齐《接口文档》3.2。
 * <p>更新接口允许只传要改的字段（null 表示不改），因此这里不用注解校验；
 * 必填判断放在 Service，区分新建与更新。
 */
@Schema(description = "新建/更新客户；更新时只传要改的字段")
public record CustomerUpsertRequest(
        @Schema(description = "客户编码，唯一，新建必填（形如 DE-0007）；更新时可改但不可与已有冲突", example = "DE-0007")
        String code,

        @Schema(description = "客户中文名（新建必填）", example = "米勒硬质合金有限公司")
        String nameCn,

        @Schema(description = "客户英文名", example = "Hartmetall Müller GmbH")
        String nameEn,

        @Schema(description = "客户类型，取字典 customer_type：distributor / end_user / trader / agent（无 oem）",
                example = "end_user",
                allowableValues = {"distributor", "end_user", "trader", "agent"})
        String customerType,

        @Schema(description = "客户等级 A/B/C/D", example = "A")
        String level,

        @Schema(description = "国家代码，ISO 3166-1 两位大写", example = "DE")
        String countryCode,

        @Schema(description = "结算币种，ISO 4217 三位大写", example = "EUR")
        String currency,

        @Schema(description = "加工材料，数组", example = "[\"stainless\",\"superalloy\"]")
        List<String> machiningMaterials,

        @Schema(description = "加工工序，数组", example = "[\"turning\",\"milling\"]")
        List<String> machiningOps,

        @Schema(description = "在用机床品牌，数组", example = "[\"DMG Mori\",\"Hermle\"]")
        List<String> machineBrands,

        @Schema(description = "对标品牌（竞品）", example = "Sandvik")
        String benchmarkBrand,

        @Schema(description = "付款条款文本", example = "30% T/T in advance")
        String paymentTerms,

        @Schema(description = "账期天数，0–365", example = "30")
        Integer creditDays,

        @Schema(description = "授信额度（原币）", example = "50000.00")
        BigDecimal creditLimit,

        @Schema(description = "归属业务员用户 id；留空默认当前登录用户")
        Long ownerId,

        @Schema(description = "备注")
        String remark) {
}