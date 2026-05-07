package com.saynow.scenario.api;

import com.saynow.common.response.ApiResponse;
import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.ScenarioDetailResponse;
import com.saynow.scenario.api.dto.ScenarioListResponse;
import com.saynow.scenario.application.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Scenario", description = "MVP 시나리오 탐색 API")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping("/categories")
    @Operation(summary = "카테고리 목록 조회", description = "활성화된 시나리오 카테고리를 정렬 순서대로 조회합니다.")
    public ApiResponse<CategoryListResponse> getCategories() {
        return ApiResponse.success(scenarioService.getCategories());
    }

    @GetMapping("/categories/{categoryId}/scenarios")
    @Operation(summary = "카테고리별 시나리오 목록 조회", description = "카테고리에 속한 활성 시나리오 목록을 조회합니다.")
    public ApiResponse<ScenarioListResponse> getScenariosByCategory(@PathVariable String categoryId) {
        return ApiResponse.success(scenarioService.getScenariosByCategory(categoryId));
    }

    @GetMapping("/scenarios/{scenarioId}")
    @Operation(summary = "시나리오 상세 조회", description = "시작 모달에 필요한 시나리오 상세 정보를 조회합니다.")
    public ApiResponse<ScenarioDetailResponse> getScenarioDetail(@PathVariable String scenarioId) {
        return ApiResponse.success(scenarioService.getScenarioDetail(scenarioId));
    }
}
