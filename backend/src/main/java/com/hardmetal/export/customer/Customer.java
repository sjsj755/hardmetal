package com.hardmetal.export.customer;

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
 * 客户主档。customer
 * <p>autoResultMap = true 是必需的：MyBatis-Plus 只有在自动结果映射下才会应用
 * {@link StringArrayTypeHandler}，否则 text[] 列查出来恒为 null。</p>
 */
@Data
@TableName(value = "customer", autoResultMap = true)
public class Customer {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String customerCode;
    private String nameCn;
    private String nameEn;
    private String shortName;
    private String countryCode;
    private String region;
    private String currencyCode;
    private String language;
    private String timezone;
    private String customerType;
    private String grade;
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] processingMaterials;
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] processes;
    private String machineBrand;
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private String[] machineBrands;
    private String machineModel;
    private String toolHolderInterface;
    private String benchmarkBrand;
    private String paymentTerms;
    private Integer creditDays;
    private BigDecimal creditLimit;
    private BigDecimal sinosureLimit;
    private String taxNo;
    private String sanctionsStatus;
    private Boolean sensitiveCountry;
    private Long ownerUserId;
    private String regionCode;
    private String sourceChannel;
    private String remark;
    @TableLogic
    private Boolean isDeleted;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private Long updatedBy;
    private OffsetDateTime updatedAt;
}