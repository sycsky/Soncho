package com.example.aikef;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AiKefApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiKefApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // 设置默认时区为日本时间 (Asia/Tokyo)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        System.out.println("Current system time in Japan: " + ZonedDateTime.now(ZoneId.of("Asia/Tokyo")));
    }
}
