package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 原料基准价。《接口文档》12.6 / 12.7。
 * <p>判重：应用层不预查，直接插入并依赖 V7 的部分唯一索引
 * {@code ux_raw_material_price_nospec}（spec IS NULL 时按 material_type + price_date 唯一），
 * 命中后翻译成 40903 —— 少一次查询，且并发下也不会重复。
 */
@Service
public class RawMaterialPriceService {

    private final RawMaterialPriceMapper rawMaterialPriceMapper;

    public RawMaterialPriceService(RawMaterialPriceMapper rawMaterialPriceMapper) {
        this.rawMaterialPriceMapper = rawMaterialPriceMapper;
    }

    public List<MaterialPriceView> list(String material, LocalDate from, LocalDate to) {
        var wrapper = Wrappers.<RawMaterialPrice>lambdaQuery()
                .eq(StringUtils.hasText(material), RawMaterialPrice::getMaterialType,
                        material == null ? null : material.trim())
                .ge(from != null, RawMaterialPrice::getPriceDate, from)
                .le(to != null, RawMaterialPrice::getPriceDate, to)
                .orderByDesc(RawMaterialPrice::getPriceDate)
                .orderByAsc(RawMaterialPrice::getMaterialType);
        return rawMaterialPriceMapper.selectList(wrapper).stream()
                .map(p -> new MaterialPriceView(p.getMaterialType(),
                        p.getPrice() == null ? null : p.getPrice().setScale(2, RoundingMode.HALF_UP),
                        p.getPriceDate(), p.getSource(), null))
                .toList();
    }

    public MaterialPriceSaved create(MaterialPriceUpsertRequest request) {
        RawMaterialPrice entity = new RawMaterialPrice();
        entity.setMaterialType(request.material().trim());
        entity.setPrice(request.priceCnyKg());
        entity.setCurrencyCode("CNY");
        entity.setPriceDate(request.date());
        entity.setSource(StringUtils.hasText(request.source()) ? request.source().trim() : "manual");
        // 不写 spec：接口不采集规格，统一按“无规格”这一档记录。
        try {
            rawMaterialPriceMapper.insert(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new BizException(ErrorCode.DUPLICATE_KEY,
                    "该日期已存在原料价：" + request.material() + " " + request.date());
        }
        return new MaterialPriceSaved(entity.getId());
    }
}