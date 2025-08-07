package com.whu.nanyin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Vue CLI 默认端口是 8080 或 8081，可以先写 8081，后续根据前端实际情况修改
        registry.addMapping("/api/**") // 允许 /api/ 下的所有请求路径
                .allowedOrigins("http://localhost:8081") // 允许这个地址的跨域请求
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true); // 允许携带凭证（如Cookie）
    }
}