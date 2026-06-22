// 로컬 개발과 테스트에서 사용할 3차 MVP 결정적 AI 대체 클라이언트
package com.saynow.session.infrastructure.ai;

import com.saynow.session.domain.InnerThoughtType;
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
                request.nextQuestion().questionKo(),
                "사용자가 답변을 이어줘서 다음 질문으로 자연스럽게 넘어가면 좋겠다.",
                InnerThoughtType.NORMAL);
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
                        null,
                        "질문에 맞는 답을 영어 문장으로 끝까지 말했기 때문이에요.",
                        null,
                        null,
                        "한국인의 35%가 틀리는 표현인데 정확히 맞췄어요."))
                .toList();
        return new AiSessionFeedbackResponse(
                request.sessionId(),
                82,
                "한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요.",
                turnFeedbacks);
    }
}
