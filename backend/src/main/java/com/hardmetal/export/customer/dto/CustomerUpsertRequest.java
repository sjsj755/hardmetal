package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新建/更新客户入参，字段名对齐《接口文档》3.2。
 * <p>更新接口允许只传要改的字段（null 表示不改），所以校验注解只对“传了值”的字段生效；
 * 必填判断放在 Service，区分新建与更新。长度上限与 DDL 列宽对齐，避免依赖数据库报错。
 */
@Schema(description = "新建/更新客户；更新时只传要改的字段")
public record CustomerUpsertRequest(
        @Schema(description = "客户编码，唯一，新建必填（形如 DE-0007）；更新时可改但不可与已有冲突", example = "DE-0007")
        @Size(max = 32, message = "客户编码最多 32 个字符")
        String code,

        @Schema(description = "客户中文名（新建必填）", example = "米勒硬质合金有限公司")
        @Size(max = 128, message = "中文名最多 128 个字符")
        String nameCn,

        @Schema(description = "客户英文名", example = "Hartmetall Müller GmbH")
        @Size(max = 128, message = "英文名最多 128 个字符")
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

        @Schema(description = "加工材料，取值见字典 machining_material（steel/stainless/cast_iron/aluminum/superalloy/composite/hardened_steel/titanium），最多 16 项",
                example = "[\"stainless\",\"superalloy\"]")
        @Size(max = 16, message = "加工材料最多 16 项")
        List<@Size(max = 64, message = "单项加工材料最多 64 个字符") String> machiningMaterials,

        @Schema(description = "加工工序，取值见字典 machining_op（turning/milling/drilling/boring/threading/parting/grooving/mold），最多 16 项",
                example = "[\"turning\",\"milling\"]")
        @Size(max = 16, message = "加工工序最多 16 项")
        List<@Size(max = 64, message = "单项加工工序最多 64 个字符") String> machiningOps,

        @Schema(description = "在用机床品牌，最多 20 项", example = "[\"DMG Mori\",\"Hermle\"]")
        @Size(max = 20, message = "机床品牌最多 20 项")
        List<@Size(max = 64, message = "单项机床品牌最多 64 个字符") String> machineBrands,

        @Schema(description = "对标品牌（竞品）", example = "Sandvik")
        @Size(max = 64, message = "对标品牌最多 64 个字符")
        String benchmarkBrand,

        @Schema(description = "付款条款文本", example = "30% T/T in advance")
        @Size(max = 32, message = "付款条款最多 32 个字符")
        String paymentTerms,

        @Schema(description = "账期天数，0–365", example = "30")
        @Min(value = 0, message = "账期天数不能为负")
        @Max(value = 365, message = "账期天数不能超过 365")
        Integer creditDays,

        @Schema(description = "授信额度（原币），不能为负", example = "50000.00")
        @DecimalMin(value = "0", message = "授信额度不能为负")
        @Digits(integer = 14, fraction = 4, message = "授信额度最多 14 位整数、4 位小数")
        BigDecimal creditLimit,

        @Schema(description = "归属业务员用户 id；留空默认当前登录用户。SALES 只能把客户归属到自己名下，目标用户必须是启用状态的 SALES 或 ADMIN")
        Long ownerId,

        @Schema(description = "备注")
        @Size(max = 2000, message = "备注最多 2000 个字符")
        String remark) {
}