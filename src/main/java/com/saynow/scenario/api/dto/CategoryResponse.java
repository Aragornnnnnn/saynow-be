// 홈 화면의 카테고리 잠금 상태와 시나리오 목록을 반환하는 DTO
package com.saynow.scenario.api.dto;

import java.util.List;

public record CategoryResponse(
        Long categoryId,
        String categoryName,
        boolean categoryLocked,
        String categoryLockReason,
        List<ScenarioResponse> scenarios
) {
}
