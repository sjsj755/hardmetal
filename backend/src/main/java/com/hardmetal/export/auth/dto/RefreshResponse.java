package com.hardmetal.export.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 刷新令牌响应。《接口文档》2.2：只返回新的三元组，不重复返回用户信息。
 */
@Schema(description = "刷新令牌响应")
public record RefreshResponse(
        @Schema(description = "新的访问令牌") String accessToken,
        @Schema(description = "新的刷新令牌，旧的立即失效") String refreshToken,
        @Schema(description = "accessToken 剩余有效秒数", example = "7200") long expiresIn) {
}