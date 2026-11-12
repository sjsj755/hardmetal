package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增/更新 HS 编码。《接口文档》12.3：按 {@code code} upsert，未传字段保持原值。
 */
public record HsCodeUpsertRequest(
        @NotBlank
        @Pattern(regexp = "^\\d{10}$", message = "HS 编码必须是 10 位数字")
        @Schema(description = "10 位 HS 编码，重复即视为更新", example = "8209009000")
        String code,

        @NotBlank
        @Size(max = 128)
        @Schema(description = "中文品名", example = "其他硬质合金制的刀具用刀片及刀头")
        String nameCn,

        @Digits(integer = 9, fraction = 6)
        @Schema(description = "出口关税，最多 6 位小数", example = "0.000000")
        BigDecimal exportTariff,

        @Digits(integer = 1, fraction = 6)
        @DecimalMax(value = "1", message = "退税率不能大于 1")
        @Schema(description = "出口退税率，刀具类锁 13%", example = "0.130000")
        BigDecimal refundRate,

        @Size(max = 64)
        @Schema(description = "监管条件", example = "两用物项和技术出口许可证")
        String regulatoryCondition,

        @Schema(description = "申报要素；空数组表示清空", example = "[\"品名\",\"材质\",\"用途\",\"牌号\"]")
        List<@Size(max = 64) String> declarationElements,

        @Size(max = 2000)
        @Schema(description = "归类依据，更新时必须保留说明")
        String classifyBasis) {
}