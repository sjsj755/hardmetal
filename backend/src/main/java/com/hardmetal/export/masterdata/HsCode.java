package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hardmetal.export.shared.mybatis.StringArrayTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * HS 编码库。《接口文档》12.2/12.3。
 * <p>{@code autoResultMap = true} 必须保留：否则查询时 {@link #declarationElements}
 * 的 typeHandler 不生效，读出来恒为 null（客户表踩过同一个坑）。
 */
@Data
@TableName(value = "hs_code", autoResultMap = true)
public class HsCode {

    /** 10 位编码，业务主键，不做自增。 */
    @TableId(value = "code", type = IdType.INPUT)
    private String code;

    private String nameCn;
    private String nameEn;
    private String unit;
    private String legalUnit1;
    private String legalUnit2;
    private BigDecimal exportDutyRate;
    private BigDecimal exportRebateRate;
    private String supervisionCondition;
    private String inspectionCategory;

    /** V7 起为 text[]，接口输出数组。 */
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] declarationElements;

    private String classificationBasis;
    private String remark;

    @TableLogic
    private Boolean isDeleted;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}