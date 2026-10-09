package com.smartkrishi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SmartKrishiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartKrishiApplication.class, args);
    }
}
