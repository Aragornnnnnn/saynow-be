// AI 최종 피드백 SSE 이벤트 스트림을 추상화한다.
package com.saynow.session.infrastructure.ai;

import reactor.core.publisher.Flux;

public interface AiFeedbackStreamClient {

    Flux<AiFeedbackStreamEvent> streamFeedback(AiFeedbackRequest request);
}
