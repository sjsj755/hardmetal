package com.hardmetal.export.masterdata;

import com.hardmetal.export.shared.web.ApiResponse;
import com.hardmetal.export.shared.web.ItemsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 港口/机场/陆路口岸。《接口文档》12.4。
 */
@Tag(name = "系统", description = "HS 编码、港口、汇率、原料基准价")
@RestController
@RequestMapping("/api/v1/ports")
public class PortController {

    private final PortService portService;

    public PortController(PortService portService) {
        this.portService = portService;
    }

    @Operation(summary = "港口列表",
            description = "type 取值 seaport / airport / landport；q 匹配编码、中文名或英文名；按编码升序。")
    @GetMapping
    public ApiResponse<ItemsResult<PortView>> list(
            @Parameter(description = "编码前缀或中英文名称关键字") @RequestParam(required = false) String q,
            @Parameter(description = "类型：seaport / airport / landport") @RequestParam(required = false) String type,
            @Parameter(description = "国家二字码，如 DE") @RequestParam(required = false) String country) {
        return ApiResponse.ok(ItemsResult.of(portService.list(q, type, country)));
    }
}