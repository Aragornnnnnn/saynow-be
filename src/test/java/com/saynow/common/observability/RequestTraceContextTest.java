// 요청 단위 trace context가 requestId와 AI 호출 시간을 보관하는지 검증한다.
package com.saynow.common.observability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestTraceContextTest {

    @AfterEach
    void clearContext() {
        RequestTraceContext.clear();
    }

    @Test
    void storesRequestIdAndAccumulatesAiCallTime() {
        RequestTraceContext.start("trace-be-123");

        long elapsedMs = RequestTraceContext.recordAiCall(1_500_000);

        assertThat(RequestTraceContext.currentRequestId()).contains("trace-be-123");
        assertThat(elapsedMs).isEqualTo(1);
        assertThat(RequestTraceContext.aiCallMs()).isEqualTo(1);
    }
}
