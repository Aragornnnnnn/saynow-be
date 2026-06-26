// 애플리케이션 시간대 설정이 한국 시간 기준으로 고정되는지 검증한다.
package com.saynow.common.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTimeZoneConfigTest {

    private TimeZone previousTimeZone;

    @BeforeEach
    void rememberDefaultTimeZone() {
        previousTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    void restoreDefaultTimeZone() {
        TimeZone.setDefault(previousTimeZone);
    }

    @Test
    void appliesConfiguredSeoulTimeZoneAsJvmDefault() {
        ApplicationTimeZoneProperties properties = new ApplicationTimeZoneProperties("Asia/Seoul");
        ApplicationTimeZoneConfig config = new ApplicationTimeZoneConfig(properties);

        config.applyDefaultTimeZone();

        assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void dateTimeProviderUsesConfiguredSeoulLocalDateTime() {
        ApplicationTimeZoneProperties properties = new ApplicationTimeZoneProperties("Asia/Seoul");
        ApplicationTimeZoneConfig config = new ApplicationTimeZoneConfig(properties);
        LocalDateTime before = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusSeconds(1);

        LocalDateTime provided = LocalDateTime.from(config.dateTimeProvider().getNow().orElseThrow());

        LocalDateTime after = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusSeconds(1);
        assertThat(provided).isBetween(before, after);
    }
}
