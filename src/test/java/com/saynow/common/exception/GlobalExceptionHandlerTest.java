// 공통 예외 핸들러의 Sentry 전송 대상을 검증하는 테스트
package com.saynow.common.exception;

import com.saynow.common.observability.SentryEventReporter;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final CapturingSentryEventReporter sentryEventReporter = new CapturingSentryEventReporter();
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler(sentryEventReporter);

    @Test
    void capturesUnexpectedException() {
        RuntimeException exception = new RuntimeException("boom");

        handler.handleUnexpectedException(exception);

        assertThat(sentryEventReporter.capturedException).isSameAs(exception);
    }

    @Test
    void capturesServerApiException() {
        ApiException exception = new ApiException(ErrorCode.AI_RESPONSE_INVALID);

        handler.handleApiException(exception);

        assertThat(sentryEventReporter.capturedException).isSameAs(exception);
    }

    @Test
    void capturesClientApiException() {
        ApiException exception = new ApiException(ErrorCode.VALIDATION_FAILED);

        handler.handleApiException(exception);

        assertThat(sentryEventReporter.capturedException).isSameAs(exception);
    }

    @Test
    void capturesValidationException() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of());

        handler.handleConstraintViolation(exception);

        assertThat(sentryEventReporter.capturedException).isSameAs(exception);
    }

    private static class CapturingSentryEventReporter implements SentryEventReporter {

        private Throwable capturedException;

        @Override
        public void captureException(Throwable exception) {
            this.capturedException = exception;
        }
    }
}
