package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 联系人视图。
 */
@Schema(description = "客户联系人")
public record ContactView(
        @Schema(description = "联系人 id") long id,
        @Schema(description = "姓名") String name,
        @Schema(description = "职位") String position,
        @Schema(description = "邮箱") String email,
        @Schema(description = "电话") String phone,
        @Schema(description = "即时通讯类型") String imType,
        @Schema(description = "即时通讯账号") String imAccount,
        @Schema(description = "沟通语言") String language,
        @Schema(description = "是否决策人") boolean isDecisionMaker) {
}