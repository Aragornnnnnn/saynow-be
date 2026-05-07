package com.saynow.scenario.controller.dto;

import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> categories
) {
}
