package com.saynow.scenario.application;

import com.saynow.common.exception.ApiException;
import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.CategoryResponse;
import com.saynow.scenario.api.dto.ScenarioDetailResponse;
import com.saynow.scenario.api.dto.ScenarioListItemResponse;
import com.saynow.scenario.api.dto.ScenarioListResponse;
import com.saynow.scenario.domain.ContentStatus;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioCategory;
import com.saynow.scenario.infrastructure.ScenarioCategoryRepository;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioCategoryRepository categoryRepository;
    private final ScenarioRepository scenarioRepository;

    public ScenarioService(ScenarioCategoryRepository categoryRepository, ScenarioRepository scenarioRepository) {
        this.categoryRepository = categoryRepository;
        this.scenarioRepository = scenarioRepository;
    }

    public CategoryListResponse getCategories() {
        return new CategoryListResponse(categoryRepository.findByStatusOrderBySortOrderAsc(ContentStatus.ACTIVE)
                .stream()
                .map(category -> new CategoryResponse(category.getCategoryKey(), category.getName(), category.getSortOrder()))
                .toList());
    }

    public ScenarioListResponse getScenariosByCategory(String categoryId) {
        ScenarioCategory category = categoryRepository.findByCategoryKeyAndStatus(categoryId, ContentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."));

        return new ScenarioListResponse(category.getCategoryKey(), scenarioRepository.findByCategoryAndStatusOrderBySortOrderAsc(category, ContentStatus.ACTIVE)
                .stream()
                .map(scenario -> new ScenarioListItemResponse(
                        scenario.getScenarioKey(),
                        scenario.getTitle(),
                        scenario.getSuccessGoal(),
                        scenario.getThumbnailUrl(),
                        scenario.getSortOrder()))
                .toList());
    }

    public ScenarioDetailResponse getScenarioDetail(String scenarioId) {
        Scenario scenario = scenarioRepository.findByScenarioKeyAndStatus(scenarioId, ContentStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SCENARIO_NOT_FOUND", "시나리오를 찾을 수 없습니다."));

        return new ScenarioDetailResponse(
                scenario.getScenarioKey(),
                scenario.getCategory().getCategoryKey(),
                scenario.getTitle(),
                scenario.getSituationDescription(),
                scenario.getSuccessGoal(),
                scenario.getOpeningBabsaeText(),
                scenario.getOpeningTtsUrl(),
                scenario.getMaxFollowUpCount());
    }
}
