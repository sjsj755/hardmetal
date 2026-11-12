package com.hardmetal.export.meta.dict;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 字典项。字段名按《接口文档》12.1 固定为 value / labelZh / labelEn / sort。
 */
public record DictItem(
        @Schema(description = "枚举值，提交与筛选都用它", example = "distributor")
        String value,

        @Schema(description = "中文标签", example = "经销商")
        String labelZh,

        @Schema(description = "英文标签；字典表暂无英文列时返回 null", example = "Distributor")
        String labelEn,

        @Schema(description = "排序号，越小越靠前", example = "10")
        Integer sort) {
}