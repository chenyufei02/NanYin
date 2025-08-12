package com.whu.nanyin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

// 测试注释2
@SpringBootApplication
@MapperScan("com.whu.nanyin.mapper")
@EnableRetry
public class NanYinApplication {

	public static void main(String[] args) {
		SpringApplication.run(NanYinApplication.class, args);
	}

}
