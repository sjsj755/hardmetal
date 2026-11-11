package com.hardmetal.export.auth;

import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import com.hardmetal.export.shared.web.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 登录态解析。命中 Bearer 令牌则校验签名、过期与吊销状态，并写入 {@link CurrentUserHolder}。
 * 免登录路径直接放行；其余路径无有效令牌返回 401。
 */
@Component
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 10)
public class AuthFilter extends OncePerRequestFilter {

    static final String HEADER = "Authorization";
    static final String PREFIX = "Bearer ";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/health",
            // 联调控制台是纯静态页，自身不含数据；上线前应删除 resources/static/dev。
            "/dev/",
            "/v3/api-docs",
            "/swagger-ui",
            "/actuator",
            "/error"
    );

    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final JsonMapper jsonMapper;

    public AuthFilter(JwtService jwtService, TokenStore tokenStore, JsonMapper jsonMapper) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.jsonMapper = jsonMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            writeError(request, response, new BizException(ErrorCode.UNAUTHORIZED));
            return;
        }

        try {
            JwtService.JwtClaims claims = jwtService.parse(header.substring(PREFIX.length()).trim());
            if (!JwtService.TYP_ACCESS.equals(claims.typ())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "该令牌不能用于接口访问，请使用 accessToken");
            }
            if (!tokenStore.isActive(claims.jti())) {
                throw new BizException(ErrorCode.UNAUTHORIZED, "会话已失效，请重新登录");
            }
            CurrentUserHolder.set(new CurrentUser(claims.userId(), claims.username(), null,
                    claims.roleCode(), null, claims.jti(), claims.expiresAt()));
            chain.doFilter(request, response);
        } catch (BizException e) {
            writeError(request, response, e);
        } finally {
            CurrentUserHolder.clear();
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private void writeError(HttpServletRequest request, HttpServletResponse response, BizException e)
            throws IOException {
        ErrorCode code = e.errorCode();
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ApiResponse<Object> body = ApiResponse.fail(code, e.getMessage(), e.data());
        response.getWriter().write(jsonMapper.writeValueAsString(body));
    }
}