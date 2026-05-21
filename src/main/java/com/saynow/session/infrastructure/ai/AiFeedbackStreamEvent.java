// AI 최종 피드백 SSE 이벤트 이름과 JSON data를 표현한다.
package com.saynow.session.infrastructure.ai;

public record AiFeedbackStreamEvent(
        String event,
        String data
) {

    public AiFeedbackStreamEvent {
        if (event == null || event.isBlank()) {
            event = "message";
        }
        if (data == null) {
            data = "";
        }
    }
}
