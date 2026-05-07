package com.saynow.scenario.api.dto;

import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> categories
) {
}
