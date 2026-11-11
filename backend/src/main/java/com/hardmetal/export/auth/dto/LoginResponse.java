package com.hardmetal.export.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应。《接口文档》2.1：accessToken(2h) + refreshToken(7d) + expiresIn(秒) + user。
 */
@Schema(description = "登录响应")
public record LoginResponse(
        @Schema(description = "访问令牌，有效期 2 小时，放入 Authorization: Bearer", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "刷新令牌，有效期 7 天，一次性轮换", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "accessToken 剩余有效秒数", example = "7200")
        long expiresIn,

        @Schema(description = "登录用户与权限点")
        UserProfile user) {
}