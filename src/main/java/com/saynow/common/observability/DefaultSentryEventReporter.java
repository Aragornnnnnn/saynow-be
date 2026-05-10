// Sentry SDK로 서버 예외 이벤트를 전송하는 reporter
package com.saynow.common.observability;

import io.sentry.Sentry;
import org.springframework.stereotype.Component;

@Component
public class DefaultSentryEventReporter implements SentryEventReporter {

    @Override
    public void captureException(Throwable exception) {
        Sentry.captureException(exception);
    }
}
