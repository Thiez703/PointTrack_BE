package com.chamcong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ChamCongHomeServicesApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChamCongHomeServicesApplication.class, args);
    }
}

