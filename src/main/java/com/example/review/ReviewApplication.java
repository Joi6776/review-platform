package com.example.review;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 内容审核平台启动类。
 *
 * 说明：现在还没有 mapper，所以先不加 @MapperScan。
 * 等第一个 Mapper 接口写出来（第 2 周），在这里加上 @MapperScan("com.example.review.mapper")。
 */
@EnableScheduling
@SpringBootApplication
public class ReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReviewApplication.class, args);
    }
}


