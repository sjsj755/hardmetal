package com.hardmetal.export.auth;

/**
 * 登录态上下文。由 AuthFilter 写入，请求结束时清理，禁止跨线程复用。
 */
public record CurrentUser(Long id, String username, String realName, String roleCode, String roleName,
                          String jti, long expiresAt) {
}