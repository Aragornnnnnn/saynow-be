// 연습 턴 평가를 수행하는 AI 클라이언트 계약
package com.saynow.practice.infrastructure.ai;

public interface AiPracticeClient {

    AiTurnEvaluationResult evaluateTurn(AiTurnEvaluationRequest request);
}
