package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "原料基准价保存结果")
public record MaterialPriceSaved(@Schema(description = "新建记录 id") Long id) {
}