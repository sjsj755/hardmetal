package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率。所有 mid/buy/sell 均为“1 单位外币 = N CNY”，基准币种固定 CNY。
 */
@Data
@TableName("currency")
public class Currency {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String currencyCode;
    private BigDecimal midRate;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
    private LocalDate rateDate;
    private String source;
    private Boolean isLocked;
}