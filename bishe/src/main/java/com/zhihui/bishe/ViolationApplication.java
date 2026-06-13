package com.zhihui.bishe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ViolationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ViolationApplication.class, args);
    }
}
