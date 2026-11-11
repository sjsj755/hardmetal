package com.hardmetal.export.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌入参。《接口文档》2.2。
 */
@Schema(description = "刷新令牌入参")
public record RefreshRequest(
        @NotBlank(message = "refreshToken 不能为空")
        @Schema(description = "登录时返回的刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken) {
}