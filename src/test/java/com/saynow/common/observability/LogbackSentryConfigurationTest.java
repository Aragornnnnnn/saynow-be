// Sentry Logback appender의 breadcrumb 수집 설정을 검증한다.
package com.saynow.common.observability;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LogbackSentryConfigurationTest {

    @Test
    void sentryAppenderCollectsInfoLogsAsBreadcrumbsInDevAndProdProfiles() throws IOException {
        String logback = Files.readString(Path.of("src/main/resources/logback-spring.xml"));

        assertThat(logback)
                .contains("<springProfile name=\"dev | prod\">")
                .contains("<minimumBreadcrumbLevel>INFO</minimumBreadcrumbLevel>")
                .contains("<minimumEventLevel>ERROR</minimumEventLevel>")
                .contains("<appender-ref ref=\"SENTRY\"/>");
    }
}
