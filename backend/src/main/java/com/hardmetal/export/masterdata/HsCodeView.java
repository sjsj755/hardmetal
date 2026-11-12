package com.hardmetal.export.masterdata;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * HS 编码列表项。字段名严格对齐《接口文档》12.2。
 *
 * @param code                10 位编码
 * @param nameCn              中文品名
 * @param nameEn              英文品名
 * @param unit1               法定第一计量单位（库中 legal_unit1，回退 unit）
 * @param exportTariff        出口关税，6 位小数字符串
 * @param refundRate          出口退税率，6 位小数字符串
 * @param regulatoryCondition 监管条件，可为空
 * @param inspectionCategory  检验检疫类别，可为空
 * @param declarationElements 申报要素
 * @param classifyBasis       归类依据
 */
public record HsCodeView(
        @Schema(description = "10 位 HS 编码") String code,
        @Schema(description = "中文品名") String nameCn,
        @Schema(description = "英文品名") String nameEn,
        @Schema(description = "法定第一计量单位") String unit1,
        @Schema(description = "出口关税，字符串 6 位小数") BigDecimal exportTariff,
        @Schema(description = "出口退税率，字符串 6 位小数；刀具类锁 13%") BigDecimal refundRate,
        @Schema(description = "监管条件，如“两用物项和技术出口许可证”") String regulatoryCondition,
        @Schema(description = "检验检疫类别") String inspectionCategory,
        @Schema(description = "申报要素列表") List<String> declarationElements,
        @Schema(description = "归类依据") String classifyBasis) {
}