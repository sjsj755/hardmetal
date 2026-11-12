package com.hardmetal.export.masterdata;

import com.hardmetal.export.auth.RequireRole;
import com.hardmetal.export.shared.web.ApiResponse;
import com.hardmetal.export.shared.web.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HS 编码库。《接口文档》12.2 / 12.3。
 */
@Tag(name = "系统", description = "HS 编码、港口、汇率、原料基准价")
@RestController
@RequestMapping("/api/v1/hs-codes")
public class HsCodeController {

    private final HsCodeService hsCodeService;

    public HsCodeController(HsCodeService hsCodeService) {
        this.hsCodeService = hsCodeService;
    }

    @Operation(summary = "HS 编码列表",
            description = "支持按编码前缀（chapter）与关键字（编码前缀/中文名/英文名）过滤，按编码升序。")
    @GetMapping
    public ApiResponse<PageResult<HsCodeView>> page(
            @Parameter(description = "页码，从 1 起") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数，最大 200") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "编码前缀或中英文名称关键字") @RequestParam(required = false) String q,
            @Parameter(description = "章节前缀，如 8207") @RequestParam(required = false) String chapter) {
        return ApiResponse.ok(hsCodeService.page(page, size, q, chapter));
    }

    @Operation(summary = "新增或更新 HS 编码",
            description = "按编码 upsert：编码不存在则新增，存在则只覆盖本次传入的字段。仅 ADMIN。")
    @PostMapping
    @RequireRole({"ADMIN"})
    public ApiResponse<HsCodeSaved> upsert(@Valid @RequestBody HsCodeUpsertRequest request) {
        return ApiResponse.ok(hsCodeService.upsert(request));
    }
}