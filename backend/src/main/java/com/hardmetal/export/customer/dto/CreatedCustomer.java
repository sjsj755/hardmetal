package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 新建客户的返回体：{@code {"id":138,"code":"DE-0007"}}。
 */
@Schema(description = "新建客户返回")
public record CreatedCustomer(
        @Schema(description = "新客户 id", example = "138") long id,
        @Schema(description = "最终使用的客户编码", example = "DE-0007") String code) {
}