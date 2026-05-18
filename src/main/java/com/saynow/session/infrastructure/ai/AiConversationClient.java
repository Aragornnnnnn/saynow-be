// 2차 MVP 백엔드가 호출하는 AI 서버 계약을 추상화한다.
package com.saynow.session.infrastructure.ai;

public interface AiConversationClient {

    AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request);

    AiFeedbackResponse generateFeedback(AiFeedbackRequest request);
}
