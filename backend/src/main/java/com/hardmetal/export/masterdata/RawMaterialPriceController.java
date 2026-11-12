package com.hardmetal.export.masterdata;

import com.hardmetal.export.auth.RequireRole;
import com.hardmetal.export.shared.web.ApiResponse;
import com.hardmetal.export.shared.web.ItemsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 原料基准价。《接口文档》12.6 / 12.7。
 */
@Tag(name = "系统", description = "HS 编码、港口、汇率、原料基准价")
@RestController
@RequestMapping("/api/v1/raw-material-prices")
public class RawMaterialPriceController {

    private final RawMaterialPriceService rawMaterialPriceService;

    public RawMaterialPriceController(RawMaterialPriceService rawMaterialPriceService) {
        this.rawMaterialPriceService = rawMaterialPriceService;
    }

    @Operation(summary = "原料价列表",
            description = "可按 material 与日期区间过滤，按日期倒序返回。")
    @GetMapping
    public ApiResponse<ItemsResult<MaterialPriceView>> list(
            @Parameter(description = "原料类型，如 tungsten_powder") @RequestParam(required = false) String material,
            @Parameter(description = "起始日期 yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "截止日期 yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(ItemsResult.of(rawMaterialPriceService.list(material, from, to)));
    }

    @Operation(summary = "录入原料价",
            description = "同一原料同一天只允许一条（spec 为空时由部分唯一索引约束），重复返回 40903。仅 ADMIN。")
    @PostMapping
    @RequireRole({"ADMIN"})
    public ApiResponse<MaterialPriceSaved> create(@Valid @RequestBody MaterialPriceUpsertRequest request) {
        return ApiResponse.ok(rawMaterialPriceService.create(request));
    }
}