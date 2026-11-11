package com.hardmetal.export.auth;

import com.hardmetal.export.auth.dto.LoginRequest;
import com.hardmetal.export.auth.dto.LoginResponse;
import com.hardmetal.export.auth.dto.LogoutRequest;
import com.hardmetal.export.auth.dto.RefreshRequest;
import com.hardmetal.export.auth.dto.RefreshResponse;
import com.hardmetal.export.auth.dto.UserProfile;
import com.hardmetal.export.shared.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "校验账号密码，签发 accessToken(2h) 与 refreshToken(7d)，返回权限点")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "刷新 Token", description = "用 refreshToken 换新的 accessToken；refreshToken 一次性轮换")
    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "登出", description = "吊销当前 accessToken，并吊销 body 中的 refreshToken")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        CurrentUser current = CurrentUserHolder.get();
        authService.logout(current == null ? null : current.jti(),
                request == null ? null : request.refreshToken());
        return ApiResponse.ok();
    }

    @Operation(summary = "当前用户", description = "返回登录人资料、角色与权限点")
    @GetMapping("/me")
    public ApiResponse<UserProfile> me() {
        return ApiResponse.ok(authService.currentProfile());
    }
}