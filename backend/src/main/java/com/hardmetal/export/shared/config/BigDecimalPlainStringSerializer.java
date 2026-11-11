package com.hardmetal.export.shared.config;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.math.BigDecimal;

/**
 * BigDecimal 一律输出为字符串，避免前端浮点精度丢失。
 * <p>用 toPlainString()，禁止科学计数法（{@code 1E+3} 前端会直接解析失败）。
 */
public class BigDecimalPlainStringSerializer extends StdSerializer<BigDecimal> {

    public BigDecimalPlainStringSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializationContext ctxt)
            throws JacksonException {
        gen.writeString(value.toPlainString());
    }
}