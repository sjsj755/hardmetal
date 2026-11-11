package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 客户时间轴事件，字段名对齐《接口文档》3.6。
 */
@Schema(description = "客户时间轴事件")
public record TimelineItem(
        @Schema(description = "发生时间，ISO-8601", example = "2026-11-12T02:00:00Z") String at,
        @Schema(description = "事件类型：quote / order / contact / note", example = "quote") String type,
        @Schema(description = "展示标题", example = "新建报价 QT-2026-0012") String title,
        @Schema(description = "关联对象 id") long refId,
        @Schema(description = "关联对象类型") String refType,
        @Schema(description = "操作人显示名") String actor) {
}