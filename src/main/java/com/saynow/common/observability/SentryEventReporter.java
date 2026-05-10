// Sentry 예외 전송 경계를 정의하는 인터페이스
package com.saynow.common.observability;

public interface SentryEventReporter {

    void captureException(Throwable exception);
}
