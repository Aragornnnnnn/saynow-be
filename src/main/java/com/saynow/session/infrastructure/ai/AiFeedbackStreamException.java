// AI 최종 피드백 SSE 호출 실패를 표현하는 예외
package com.saynow.session.infrastructure.ai;

public class AiFeedbackStreamException extends RuntimeException {

    public AiFeedbackStreamException(String message) {
        super(message);
    }

    public AiFeedbackStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
