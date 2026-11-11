package com.hardmetal.export.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增联系人入参。
 */
@Schema(description = "新增客户联系人")
public record ContactUpsertRequest(
        @NotBlank(message = "联系人姓名不能为空")
        @Size(max = 64, message = "联系人姓名最长 64 字符")
        @Schema(description = "姓名", example = "Thomas Müller")
        String name,

        @Schema(description = "职位", example = "采购经理")
        String position,

        @Email(message = "邮箱格式不正确")
        @Schema(description = "邮箱", example = "t.mueller@example.de")
        String email,

        @Schema(description = "电话", example = "+49 89 123456")
        String phone,

        @Schema(description = "即时通讯类型", example = "whatsapp")
        String imType,

        @Schema(description = "即时通讯账号")
        String imAccount,

        @Schema(description = "沟通语言", example = "en")
        String language,

        @Schema(description = "是否决策人", example = "true")
        Boolean isDecisionMaker) {
}