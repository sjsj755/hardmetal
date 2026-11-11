package com.hardmetal.export.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "登录入参")
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名过长")
        @Schema(description = "登录名", example = "admin")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(max = 64, message = "密码过长")
        @Schema(description = "明文密码（走 HTTPS）", example = "Admin@2026")
        String password
) {
}