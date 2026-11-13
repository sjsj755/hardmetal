package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户详情，字段名对齐《接口文档》3.3：档案 + 联系人 + 价格摘要 + 最近报价 + 订单统计。
 */
@Schema(description = "客户详情，聚合联系人、价格摘要、最近报价与订单统计")
public record CustomerDetailView(
        @Schema(description = "客户 id") long id,
        @Schema(description = "客户编码", example = "DE-0007") String code,
        @Schema(description = "中文名") String nameCn,
        @Schema(description = "英文名") String nameEn,
        @Schema(description = "客户类型") String customerType,
        @Schema(description = "等级 A/B/C/D") String level,
        @Schema(description = "国家代码") String countryCode,
        @Schema(description = "结算币种") String currency,
        @Schema(description = "加工材料") List<String> machiningMaterials,
        @Schema(description = "加工工序") List<String> machiningOps,
        @Schema(description = "在用机床品牌") List<String> machineBrands,
        @Schema(description = "对标品牌") String benchmarkBrand,
        @Schema(description = "付款条款") String paymentTerms,
        @Schema(description = "账期天数") Integer creditDays,
        @Schema(description = "授信额度") BigDecimal creditLimit,
        @Schema(description = "归属用户 id；无归属时为 null", example = "2")
        Long ownerId,
        @Schema(description = "归属用户显示名：优先真实姓名，为空则用户名；无归属时为 null", example = "张三")
        String ownerName,
        @Schema(description = "制裁筛查状态：unreviewed=未筛查（当前无筛查流程，P0 固定返回此值）/ clear / review / blocked")
        String sanctionsStatus,
        @Schema(description = "联系人列表") List<ContactView> contacts,
        @Schema(description = "价格表摘要") PriceSummary priceSummary,
        @Schema(description = "最近报价，最多 5 条") List<RecentQuote> recentQuotes,
        @Schema(description = "累计订单数") long orderCount,
        @Schema(description = "最近下单日期") java.time.LocalDate lastOrderAt,
        @Schema(description = "备注，最多 2000 字；更新时传空串表示清空", example = "老客户，价格敏感")
        String remark) {
}