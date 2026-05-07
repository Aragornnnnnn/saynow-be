package com.saynow.scenario.controller.dto;

public record CategoryResponse(
        String categoryId,
        String name,
        int sortOrder
) {
}
