// 완료된 세션의 시나리오 성공 또는 실패 결과를 반환하는 DTO
package com.saynow.session.api.dto;

public record SessionResultResponse(
        String scenarioResult
) {
}
