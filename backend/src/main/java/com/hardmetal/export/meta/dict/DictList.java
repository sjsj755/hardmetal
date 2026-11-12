package com.hardmetal.export.meta.dict;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 字典查询响应体：{@code {"items": [...]}}，与《接口文档》12.1 一致。
 */
public record DictList(
        @Schema(description = "字典项列表，按 sort 升序")
        List<DictItem> items) {
}