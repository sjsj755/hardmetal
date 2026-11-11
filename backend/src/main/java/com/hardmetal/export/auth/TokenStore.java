package com.hardmetal.export.auth;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 令牌登记表（Redis）。
 * <ul>
 *   <li>{@code auth:token:<jti>} —— access token 白名单，登出即删。</li>
 *   <li>{@code auth:refresh:<jti>} —— refresh token，一次性使用：刷新时先删后发新。</li>
 * </ul>
 * 两处 TTL 都等于令牌自身剩余有效期，过期自动清理。
 */
@Component
public class TokenStore {

    private static final String ACCESS_PREFIX = "auth:token:";
    private static final String REFRESH_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redis;

    public TokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void registerAccess(String jti, Long userId, long ttlSeconds) {
        redis.opsForValue().set(ACCESS_PREFIX + jti, String.valueOf(userId), Duration.ofSeconds(ttlSeconds));
    }

    public boolean isActive(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(ACCESS_PREFIX + jti));
    }

    public void revokeAccess(String jti) {
        redis.delete(ACCESS_PREFIX + jti);
    }

    public void registerRefresh(String jti, Long userId, long ttlSeconds) {
        redis.opsForValue().set(REFRESH_PREFIX + jti, String.valueOf(userId), Duration.ofSeconds(ttlSeconds));
    }

    /** 一次性消费：存在则删除并返回 true，已被用过或已过期返回 false。 */
    public boolean consumeRefresh(String jti) {
        return Boolean.TRUE.equals(redis.delete(REFRESH_PREFIX + jti));
    }

    public boolean hasRefresh(String jti) {
        return Boolean.TRUE.equals(redis.hasKey(REFRESH_PREFIX + jti));
    }
}