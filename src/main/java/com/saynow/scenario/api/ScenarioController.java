package com.saynow.scenario.api;

import com.saynow.common.response.ApiResponse;
import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.ScenarioDetailResponse;
import com.saynow.scenario.api.dto.ScenarioListResponse;
import com.saynow.scenario.application.ScenarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ScenarioController {

    private final ScenarioService scenarioService;

    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @GetMapping("/categories")
    public ApiResponse<CategoryListResponse> getCategories() {
        return ApiResponse.success(scenarioService.getCategories());
    }

    @GetMapping("/categories/{categoryId}/scenarios")
    public ApiResponse<ScenarioListResponse> getScenariosByCategory(@PathVariable String categoryId) {
        return ApiResponse.success(scenarioService.getScenariosByCategory(categoryId));
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ApiResponse<ScenarioDetailResponse> getScenarioDetail(@PathVariable String scenarioId) {
        return ApiResponse.success(scenarioService.getScenarioDetail(scenarioId));
    }
}
