// 카테고리별 시나리오 목록을 감싸는 응답 DTO
package com.saynow.scenario.api.dto;

import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> categories
) {
}
