// AI 서버의 다음 꼬리 질문 생성 결과 DTO
package com.saynow.session.infrastructure.ai;

import java.util.List;

public record AiNextQuestionResponse(
        String nextQuestion,
        String translatedQuestion,
        String nextQuestionTargetSlotName,
        List<AiFilledSlot> filledSlots,
        TurnClassification turnClassification
) {

    public AiNextQuestionResponse(
            String nextQuestion,
            String translatedQuestion,
            List<AiFilledSlot> filledSlots,
            TurnClassification turnClassification
    ) {
        this(nextQuestion, translatedQuestion, null, filledSlots, turnClassification);
    }
}
