// 애플리케이션 시간대 설정 값을 보관한다.
package com.saynow.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.ZoneId;

@ConfigurationProperties(prefix = "saynow.time")
public record ApplicationTimeZoneProperties(String zone) {

    private static final String DEFAULT_ZONE = "Asia/Seoul";

    public ApplicationTimeZoneProperties {
        if (zone == null || zone.isBlank()) {
            zone = DEFAULT_ZONE;
        }
        ZoneId.of(zone);
    }

    public ZoneId zoneId() {
        return ZoneId.of(zone);
    }
}
