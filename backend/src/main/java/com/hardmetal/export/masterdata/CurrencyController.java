package com.hardmetal.export.masterdata;

import com.hardmetal.export.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 汇率查询。《接口文档》12.5。
 */
@Tag(name = "系统", description = "HS 编码、港口、汇率、原料基准价")
@RestController
@RequestMapping("/api/v1/currencies")
public class CurrencyController {

    private final CurrencyService currencyService;

    public CurrencyController(CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Operation(summary = "汇率查询",
            description = "date 缺省取库中最新汇率日；base 当前仅支持 CNY；返回的 items 不含基准币种本身。")
    @GetMapping
    public ApiResponse<CurrencyList> list(
            @Parameter(description = "汇率日期 yyyy-MM-dd，缺省为最新一天")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "基准币种，当前仅支持 CNY") @RequestParam(required = false) String base) {
        return ApiResponse.ok(currencyService.list(date, base));
    }
}