package com.saynow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SaynowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaynowApplication.class, args);
    }
}
