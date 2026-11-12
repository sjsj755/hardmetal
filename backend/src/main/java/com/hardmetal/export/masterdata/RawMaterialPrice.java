package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 原料基准价（钨粉/碳化钨/钴粉等），按周采集。
 * <p>唯一性：{@code (material_type, spec, price_date)}；spec 为空的行另有 V7 的部分唯一索引
 * {@code ux_raw_material_price_nospec} 兜底（PG 中 NULL 不参与普通唯一约束）。
 */
@Data
@TableName("raw_material_price")
public class RawMaterialPrice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** tungsten_powder / wc_powder / cobalt_powder / titanium / tantalum */
    private String materialType;

    private String spec;
    private String purity;
    private BigDecimal price;
    private String currencyCode;
    private LocalDate priceDate;
    private String source;
    private BigDecimal changePct;
}