package com.hardmetal.export.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登出入参。《接口文档》2.3：body 只带要吊销的 refreshToken；accessToken 从请求头取。
 * 允许为空，兼容旧前端不带 body 的调用。
 */
@Schema(description = "登出入参")
public record LogoutRequest(
        @Schema(description = "要吊销的刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken) {
}