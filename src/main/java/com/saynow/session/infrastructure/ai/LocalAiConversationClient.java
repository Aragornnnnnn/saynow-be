// 로컬 개발과 테스트에서 사용할 3차 MVP 결정적 AI 대체 클라이언트
package com.saynow.session.infrastructure.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "saynow.ai", name = "client-mode", havingValue = "local", matchIfMissing = true)
public class LocalAiConversationClient implements AiConversationClient {

    @Override
    public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
        return new AiNextQuestionResponse(
                request.nextQuestion().questionEn(),
                request.nextQuestion().questionKo());
    }

    @Override
    public AiTurnFeedbackStatusResponse generateTurnFeedback(AiTurnFeedbackRequest request) {
        return new AiTurnFeedbackStatusResponse(
                request.sessionId(),
                request.turnId(),
                TurnFeedbackStatus.PREPARING);
    }

    @Override
    public AiSessionFeedbackResponse generateSessionFeedback(AiSessionFeedbackRequest request) {
        List<AiSessionTurnFeedbackResponse> turnFeedbacks = request.expectedTurnIds().stream()
                .map(turnId -> new AiSessionTurnFeedbackResponse(
                        turnId,
                        FeedbackType.GOOD,
                        "한국어로 비유하자면 하고 싶은 말을 담백하게 전달한 느낌이에요.",
                        "질문에 맞는 답을 영어 문장으로 끝까지 말했기 때문이에요.",
                        null))
                .toList();
        return new AiSessionFeedbackResponse(
                request.sessionId(),
                82,
                "유학생 수준",
                "하고 싶은 말을 끝까지 전달하는 힘이 좋았고, 이유를 덧붙이는 문장도 자연스러웠어요.",
                turnFeedbacks);
    }
}
