// 애플리케이션과 JPA 감사 시간의 기준 시간대를 설정한다.
package com.saynow.common.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.TimeZone;

@Configuration
@EnableConfigurationProperties(ApplicationTimeZoneProperties.class)
@RequiredArgsConstructor
public class ApplicationTimeZoneConfig {

    private final ApplicationTimeZoneProperties properties;

    @PostConstruct
    void applyDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(properties.zoneId()));
    }

    @Bean
    DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(properties.zoneId()));
    }
}
