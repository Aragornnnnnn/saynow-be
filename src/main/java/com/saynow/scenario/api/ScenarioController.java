package com.saynow.scenario.api;

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
    public CategoryListResponse getCategories() {
        return scenarioService.getCategories();
    }

    @GetMapping("/categories/{categoryId}/scenarios")
    public ScenarioListResponse getScenariosByCategory(@PathVariable String categoryId) {
        return scenarioService.getScenariosByCategory(categoryId);
    }

    @GetMapping("/scenarios/{scenarioId}")
    public ScenarioDetailResponse getScenarioDetail(@PathVariable String scenarioId) {
        return scenarioService.getScenarioDetail(scenarioId);
    }
}
