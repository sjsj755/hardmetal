package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "HS 编码保存结果")
public record HsCodeSaved(@Schema(description = "10 位 HS 编码") String code) {
}