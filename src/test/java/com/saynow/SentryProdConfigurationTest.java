// 운영 Sentry 설정이 SDK 옵션으로 바인딩되는지 검증하는 테스트
package com.saynow;

import io.sentry.SentryOptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.profiles.active=prod",
        "spring.datasource.url=jdbc:h2:mem:saynow-sentry-prod;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "sentry.dsn=https://public@example.com/1",
        "sentry.environment=prod-test",
        "sentry.release=saynow-be-test",
        "sentry.logs.enabled=true"
})
@AutoConfigureMockMvc
class SentryProdConfigurationTest {

    @Autowired
    private SentryOptions sentryOptions;

    @Test
    void prodProfileBindsSentryOptions() {
        assertThat(sentryOptions.getDsn()).isEqualTo("https://public@example.com/1");
        assertThat(sentryOptions.getEnvironment()).isEqualTo("prod-test");
        assertThat(sentryOptions.getRelease()).isEqualTo("saynow-be-test");
        assertThat(sentryOptions.getMaxRequestBodySize()).isEqualTo(SentryOptions.RequestSize.NONE);
        assertThat(sentryOptions.getTracesSampleRate()).isZero();
        assertThat(sentryOptions.isSendDefaultPii()).isFalse();
        assertThat(sentryOptions.getLogs().isEnabled()).isTrue();
    }
}
