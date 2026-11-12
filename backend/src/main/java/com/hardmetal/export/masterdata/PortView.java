package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 港口/机场/陆路口岸。字段名对齐《接口文档》12.4。
 *
 * @param code     UN/LOCODE
 * @param name     英文名
 * @param nameCn   中文名
 * @param country  ISO 两位国家代码
 * @param type     seaport / airport / landport
 */
public record PortView(
        @Schema(description = "UN/LOCODE，如 DEHAM") String code,
        @Schema(description = "英文名称") String name,
        @Schema(description = "中文名称") String nameCn,
        @Schema(description = "国家代码 ISO 两位") String country,
        @Schema(description = "类型：seaport / airport / landport") String type) {
}