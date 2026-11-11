package com.hardmetal.export.shared.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运维健康检查。不属于业务契约，仅用于确认服务与统一响应链路可用。
 */
@Tag(name = "系统")
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @Operation(summary = "健康检查", description = "返回服务状态与当前 traceId，用于确认统一响应链路")
    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "hardmetal-export");
        body.put("time", OffsetDateTime.now().toString());
        return ApiResponse.ok(body);
    }
}