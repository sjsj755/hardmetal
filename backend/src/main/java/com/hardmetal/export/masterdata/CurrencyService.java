package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 汇率查询。《接口文档》12.5：基准币种当前仅支持 CNY，{@code date} 缺省取库中最新一天。
 * <p>返回的 items 不含基准币种本身（与契约示例一致）。
 */
@Service
public class CurrencyService {

    private static final String BASE = "CNY";

    private final CurrencyMapper currencyMapper;

    public CurrencyService(CurrencyMapper currencyMapper) {
        this.currencyMapper = currencyMapper;
    }

    public CurrencyList list(LocalDate date, String base) {
        String b = base == null || base.isBlank() ? BASE : base.trim().toUpperCase();
        if (!BASE.equals(b)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "当前仅支持 CNY 基准，收到：" + base);
        }
        LocalDate d = date;
        if (d == null) {
            Currency latest = currencyMapper.selectOne(Wrappers.<Currency>lambdaQuery()
                    .orderByDesc(Currency::getRateDate)
                    .last("limit 1"));
            d = latest == null ? null : latest.getRateDate();
        }
        if (d == null) {
            return new CurrencyList(null, b, List.of());
        }
        List<CurrencyView> items = currencyMapper.selectList(Wrappers.<Currency>lambdaQuery()
                        .eq(Currency::getRateDate, d)
                        .ne(Currency::getCurrencyCode, BASE)
                        .orderByAsc(Currency::getCurrencyCode))
                .stream()
                .map(c -> new CurrencyView(trim(c.getCurrencyCode()), c.getMidRate(), c.getBuyRate(),
                        c.getSellRate(), c.getSource()))
                .toList();
        return new CurrencyList(d, b, items);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}