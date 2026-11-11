package com.hardmetal.export.shared.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;

/**
 * Jackson 全局约定。
 * <p>金额、税率、费率统一以字符串传输（《接口文档》0.5），前端按 BigDecimal 解析，全链路无浮点。
 */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer hardmetalJsonCustomizer() {
        SimpleModule module = new SimpleModule("hardmetal-decimal");
        module.addSerializer(BigDecimal.class, new BigDecimalPlainStringSerializer());
        return builder -> builder.addModule(module);
    }
}