package com.hardmetal.export;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 硬质合金刀具出口系统后端启动类。
 */
@SpringBootApplication
@MapperScan("com.hardmetal.export")
public class ExportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExportApplication.class, args);
    }
}