package com.saynow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
@ConfigurationPropertiesScan
@SpringBootApplication
public class SaynowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SaynowApplication.class, args);
    }
}
