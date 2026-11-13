package com.hardmetal.export.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.hardmetal.export.auth.dto.LoginRequest;
import com.hardmetal.export.auth.dto.LoginResponse;
import com.hardmetal.export.auth.dto.RefreshResponse;
import com.hardmetal.export.auth.dto.UserProfile;
import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import com.hardmetal.export.system.role.SysRole;
import com.hardmetal.export.system.role.SysRoleMapper;
import com.hardmetal.export.system.user.SysUser;
import com.hardmetal.export.system.user.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * 登录、刷新、登出与当前用户。
 * <p>令牌模型严格按《接口文档》2.1–2.3：</p>
 * <ul>
 *   <li>access token 2h，登记 {@code auth:token:<jti>}，接口鉴权只看它；</li>
 *   <li>refresh token 7d，登记 {@code auth:refresh:<jti>}，用一次即删（一次性轮换）；</li>
 *   <li>登出同时吊销当前 access 与传入的 refresh。</li>
 * </ul>
 * 失败计数策略：连续 5 次密码错误锁定 15 分钟，成功登录清零。
 */
@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenStore tokenStore;
    private final int maxFailed;
    private final int lockMinutes;

    public AuthService(SysUserMapper userMapper,
                       SysRoleMapper roleMapper,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenStore tokenStore,
                       @Value("${app.auth.max-failed:5}") int maxFailed,
                       @Value("${app.auth.lock-minutes:15}") int lockMinutes) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
        this.maxFailed = maxFailed;
        this.lockMinutes = lockMinutes;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, request.username()));
        if (user == null) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now)) {
            throw new BizException(ErrorCode.BAD_CREDENTIALS,
                    "密码错误次数过多，账号锁定至 " + user.getLockedUntil());
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordFailure(user, now);
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }

        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userMapper.updateById(user);

        IssuedPair pair = issuePair(user, roleCode(user.getRoleId()));
        return new LoginResponse(pair.accessToken(), pair.refreshToken(),
                jwtService.accessTtlSeconds(), profile(user));
    }

    /**
     * 刷新：refresh token 必须 typ=refresh 且在 Redis 中仍存在；消费后立即作废旧令牌并发新对。
     */
    public RefreshResponse refresh(String refreshToken) {
        JwtService.JwtClaims claims;
        try {
            claims = jwtService.parse(refreshToken);
        } catch (BizException e) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "refreshToken 已过期或无效");
        }
        if (!JwtService.TYP_REFRESH.equals(claims.typ())) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "refreshToken 无效");
        }
        if (!tokenStore.consumeRefresh(claims.jti())) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED, "refreshToken 已被使用或已吊销");
        }
        SysUser user = userMapper.selectById(claims.userId());
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不存在或已停用");
        }
        IssuedPair pair = issuePair(user, roleCode(user.getRoleId()));
        return new RefreshResponse(pair.accessToken(), pair.refreshToken(), jwtService.accessTtlSeconds());
    }

    /**
     * 登出：吊销当前 access jti；若带了 refreshToken 一并吊销。
     * refreshToken 无效或已用过不算错误，保证幂等。
     */
    public void logout(String accessJti, String refreshToken) {
        if (accessJti != null) {
            tokenStore.revokeAccess(accessJti);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                JwtService.JwtClaims claims = jwtService.parse(refreshToken);
                if (JwtService.TYP_REFRESH.equals(claims.typ())) {
                    tokenStore.consumeRefresh(claims.jti());
                }
            } catch (BizException ignored) {
                // 过期或伪造的 refreshToken：登出仍然成功
            }
        }
    }

    public UserProfile currentProfile() {
        CurrentUser current = CurrentUserHolder.require();
        SysUser user = userMapper.selectById(current.id());
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "账号不存在或已删除");
        }
        return profile(user);
    }

    private IssuedPair issuePair(SysUser user, String roleCode) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        String access = jwtService.issue(user.getId(), user.getUsername(), roleCode, accessJti,
                JwtService.TYP_ACCESS, jwtService.accessTtlSeconds());
        String refresh = jwtService.issue(user.getId(), user.getUsername(), roleCode, refreshJti,
                JwtService.TYP_REFRESH, jwtService.refreshTtlSeconds());
        tokenStore.registerAccess(accessJti, user.getId(), jwtService.accessTtlSeconds());
        tokenStore.registerRefresh(refreshJti, user.getId(), jwtService.refreshTtlSeconds());
        return new IssuedPair(access, refresh);
    }

    private void recordFailure(SysUser user, OffsetDateTime now) {
        int failed = (user.getFailedLoginCount() == null ? 0 : user.getFailedLoginCount()) + 1;
        user.setFailedLoginCount(failed);
        if (failed >= maxFailed) {
            user.setLockedUntil(now.plusMinutes(lockMinutes));
            user.setFailedLoginCount(0);
        }
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    private String roleCode(Long roleId) {
        SysRole role = roleId == null ? null : roleMapper.selectById(roleId);
        return role == null ? "UNKNOWN" : role.getCode();
    }

    private UserProfile profile(SysUser user) {
        SysRole role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "UNKNOWN" : role.getCode();
        return new UserProfile(user.getId(), user.getUsername(), user.getRealName(),
                roleCode,
                user.getDepartment(), user.getPhone(), user.getEmail(), user.getLastLoginAt(),
                RolePerms.codes(roleCode));
    }

    private record IssuedPair(String accessToken, String refreshToken) {
    }
}