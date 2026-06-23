// 대화 종료 메시지 생성을 요청하는 AI 서버 payload
package com.saynow.session.infrastructure.ai;

public record AiClosingMessageRequest(
        Long sessionId,
        Long submittedTurnId,
        int submittedSequence,
        AiScenarioContext scenario,
        AiTurnContext currentTurn,
        ClosingReason closingReason,
        GoalCompletionStatus goalCompletionStatus
) {
}
