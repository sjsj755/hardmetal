package com.hardmetal.export.shared.web;

import com.hardmetal.export.shared.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维健康检查。不属于业务契约。
 * <p>会真实探测 PostgreSQL 与 Redis：任一不可用时返回 HTTP 503 + code=50001，避免“数据库挂了还报 UP”。
 * <p>同时提供 {@code /health} 简写；更详细的组件状态见 Actuator 的 {@code /actuator/health}。
 */
@Tag(name = "系统")
@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "健康检查", description = "探测数据库与 Redis；任一下线返回 503。响应体只给状态，不暴露连接串等细节。")
    @GetMapping({"/api/v1/health", "/health"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> db = component("database", this::probeDatabase);
        Map<String, Object> redis = component("redis", this::probeRedis);
        boolean up = "UP".equals(db.get("status")) && "UP".equals(redis.get("status"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", up ? "UP" : "DOWN");
        body.put("service", "hardmetal-export");
        body.put("time", OffsetDateTime.now().toString());
        body.put("components", Map.of("database", db, "redis", redis));
        if (up) {
            return ResponseEntity.ok(ApiResponse.ok(body));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR, "依赖不可用", body));
    }

    private Map<String, Object> component(String name, Check check) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            check.run();
            result.put("status", "UP");
        } catch (Exception ex) {
            // 详情只进日志，响应里不带主机名/账号等连接信息（该接口匿名可访问）
            log.warn("health check failed: {}", name, ex);
            result.put("status", "DOWN");
        }
        return result;
    }

    private void probeDatabase() {
        Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
        if (one == null || one != 1) {
            throw new IllegalStateException("unexpected select 1 result");
        }
    }

    private void probeRedis() {
        String pong = redisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
        if (!"PONG".equalsIgnoreCase(pong)) {
            throw new IllegalStateException("unexpected redis ping result");
        }
    }

    @FunctionalInterface
    private interface Check {
        void run();
    }
}