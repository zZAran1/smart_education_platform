package com.example.smart_education_platform_backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@MapperScan("com.example.smart_education_platform_backend.mapper")
@SpringBootApplication
public class SmartEducationPlatformBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartEducationPlatformBackendApplication.class, args);
    }

}
