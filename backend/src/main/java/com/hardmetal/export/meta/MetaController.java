package com.hardmetal.export.meta;

import com.hardmetal.export.meta.dict.DictList;
import com.hardmetal.export.meta.dict.DictService;
import com.hardmetal.export.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统与基础资料。当前只有字典查询，《接口文档》12.2 起的 HS 编码等尚未实现。
 */
@RestController
@RequestMapping("/api/v1/meta")
@Tag(name = "系统", description = "字典与基础资料")
public class MetaController {

    private final DictService dictService;

    public MetaController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/dict/{type}")
    @Operation(summary = "字典查询", description = "返回指定字典类型的 value/labelZh/labelEn/sort，按 sort 升序；类型不存在抛 40401。")
    public ApiResponse<DictList> dict(
            @Parameter(description = "字典类型，如 customer_type / incoterm / coating", example = "customer_type")
            @PathVariable String type) {
        return ApiResponse.ok(new DictList(dictService.list(type)));
    }
}