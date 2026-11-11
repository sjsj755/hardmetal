package com.hardmetal.export.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 文档元信息。契约唯一事实源是 contract/openapi.yaml，此处输出视为派生品。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI hardmetalOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("硬质合金刀具出口系统 API")
                        .version("v0.9")
                        .description("统一响应 {code,msg,data,traceId}；金额 string 2 位、税率 string 6 位。")
                        .contact(new Contact().name("hardmetal export")))
                .servers(List.of(new Server().url("/").description("当前环境")));
    }
}