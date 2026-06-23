// AI 서버가 생성한 대화 종료 멘트와 마지막 속마음 응답
package com.saynow.session.infrastructure.ai;

import com.saynow.session.domain.InnerThoughtType;

public record AiClosingMessageResponse(
        String aiMessage,
        String translatedMessage,
        String innerThought,
        InnerThoughtType innerThoughtType
) {
}
