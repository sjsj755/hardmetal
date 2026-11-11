package com.hardmetal.export.auth;

import com.hardmetal.export.shared.exception.BizException;
import com.hardmetal.export.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 手写 HS256 JWT：只依赖 JDK 与 Jackson，不引入第三方 JWT 库。
 * 载荷固定六个键：sub(userId) / usr / rol / jti / typ(access|refresh) / exp。
 * access 与 refresh 用同一密钥、不同 TTL，靠 typ 区分用途，避免拿 refresh 直接调业务接口。
 */
@Component
public class JwtService {

    private static final Base64.Encoder B64U = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64U_DEC = Base64.getUrlDecoder();
    private static final String HEADER = B64U.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    public static final String TYP_ACCESS = "access";
    public static final String TYP_REFRESH = "refresh";

    private final byte[] secret;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final JsonMapper jsonMapper;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-seconds:7200}") long accessTtlSeconds,
                      @Value("${app.jwt.refresh-ttl-seconds:604800}") long refreshTtlSeconds,
                      JsonMapper jsonMapper) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少 32 字节");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTtlSeconds = accessTtlSeconds;
        this.refreshTtlSeconds = refreshTtlSeconds;
        this.jsonMapper = jsonMapper;
    }

    public long accessTtlSeconds() {
        return accessTtlSeconds;
    }

    public long refreshTtlSeconds() {
        return refreshTtlSeconds;
    }

    public String issue(Long userId, String username, String roleCode, String jti, String typ, long ttlSeconds) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(userId));
        claims.put("usr", username);
        claims.put("rol", roleCode);
        claims.put("jti", jti);
        claims.put("typ", typ);
        claims.put("exp", Instant.now().getEpochSecond() + ttlSeconds);

        String payload = B64U.encodeToString(jsonMapper.writeValueAsBytes(claims));
        String signingInput = HEADER + "." + payload;
        return signingInput + "." + B64U.encodeToString(sign(signingInput));
    }

    public JwtClaims parse(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌格式错误");
        }
        String signingInput = parts[0] + "." + parts[1];
        byte[] expected = sign(signingInput);
        byte[] actual = B64U_DEC.decode(parts[2]);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "令牌签名无效");
        }
        Map<?, ?> claims = jsonMapper.readValue(B64U_DEC.decode(parts[1]), Map.class);
        long exp = ((Number) claims.get("exp")).longValue();
        if (Instant.now().getEpochSecond() >= exp) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        String typ = (String) claims.get("typ");
        if (typ == null) {
            typ = TYP_ACCESS;
        }
        return new JwtClaims(Long.valueOf((String) claims.get("sub")),
                (String) claims.get("usr"),
                (String) claims.get("rol"),
                (String) claims.get("jti"),
                typ,
                exp);
    }

    private byte[] sign(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 签名失败", e);
        }
    }

    public record JwtClaims(Long userId, String username, String roleCode, String jti,
                            String typ, long expiresAt) {
    }
}