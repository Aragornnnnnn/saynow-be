// 요청 단위 requestId와 AI 호출 누적 시간을 보관하는 trace context
package com.saynow.common.observability;

import org.slf4j.MDC;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class RequestTraceContext {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final ThreadLocal<RequestTiming> REQUEST_TIMING = new ThreadLocal<>();

    private RequestTraceContext() {
    }

    public static void start(String requestId) {
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        REQUEST_TIMING.set(new RequestTiming());
    }

    public static Optional<String> currentRequestId() {
        return Optional.ofNullable(MDC.get(REQUEST_ID_MDC_KEY));
    }

    public static long recordAiCall(long elapsedNanos) {
        RequestTiming timing = REQUEST_TIMING.get();
        if (timing != null) {
            timing.addAiCallNanos(elapsedNanos);
        }
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    }

    public static long aiCallMs() {
        RequestTiming timing = REQUEST_TIMING.get();
        if (timing == null) {
            return 0;
        }
        return TimeUnit.NANOSECONDS.toMillis(timing.aiCallNanos());
    }

    public static void clear() {
        REQUEST_TIMING.remove();
        MDC.remove(REQUEST_ID_MDC_KEY);
    }

    private static final class RequestTiming {

        private long aiCallNanos;

        private void addAiCallNanos(long elapsedNanos) {
            aiCallNanos += elapsedNanos;
        }

        private long aiCallNanos() {
            return aiCallNanos;
        }
    }
}
