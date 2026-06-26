// 맞장구와 다음 고정 질문 결합을 위해 AI 서버에 전달하는 요청 DTO
package com.saynow.session.infrastructure.ai;

public record AiNextQuestionRequest(
        Long sessionId,
        Long submittedTurnId,
        int submittedSequence,
        AiNextQuestionScenarioContext scenario,
        AiTurnContext currentTurn,
        AiFixedQuestion nextQuestion
) {
}
