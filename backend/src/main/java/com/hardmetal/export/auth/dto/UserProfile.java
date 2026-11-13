package com.hardmetal.export.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 当前登录用户。perms 供前端生成菜单与按钮，token 里不放权限（《接口文档》2.1 注意事项）。
 */
@Schema(description = "当前登录用户")
public record UserProfile(
        @Schema(description = "用户 id", example = "1") Long id,
        @Schema(description = "登录名", example = "admin") String username,
        @Schema(description = "姓名", example = "系统管理员") String realName,
        @Schema(description = "角色代码", example = "ADMIN") String roleCode,
        @Schema(description = "部门", example = "信息部") String department,
        @Schema(description = "手机号") String phone,
        @Schema(description = "邮箱") String email,
        @Schema(description = "最近登录时间") OffsetDateTime lastLoginAt,
        @Schema(description = "权限点列表，供前端生成菜单与按钮", example = "[\"customer:read\",\"customer:write\"]")
        List<String> perms) {
}