// 사용자 발화 처리 후 제출 턴 상태와 다음 턴을 반환하는 DTO
package com.saynow.session.api.dto;

public record UserUtteranceResponse(
        SubmittedTurnResponse submittedTurn,
        SessionTurnResponse nextTurn,
        SessionProgressResponse progress
) {
}
