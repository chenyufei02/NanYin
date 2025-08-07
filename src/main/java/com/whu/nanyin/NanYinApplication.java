package com.whu.nanyin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.whu.nanyin.mapper")
@EnableScheduling
@EnableRetry
public class NanYinApplication {

	public static void main(String[] args) {
		SpringApplication.run(NanYinApplication.class, args);
	}

}
