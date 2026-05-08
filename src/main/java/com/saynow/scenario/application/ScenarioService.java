package com.saynow.scenario.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.CategoryResponse;
import com.saynow.scenario.api.dto.ScenarioDetailResponse;
import com.saynow.scenario.api.dto.ScenarioListItemResponse;
import com.saynow.scenario.api.dto.ScenarioListResponse;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioCategory;
import com.saynow.scenario.infrastructure.ScenarioCategoryRepository;
import com.saynow.scenario.infrastructure.ScenarioRepository;
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
        return new CategoryListResponse(categoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(category -> new CategoryResponse(category.getCategoryKey(), category.getName()))
                .toList());
    }

    public ScenarioListResponse getScenariosByCategory(String categoryId) {
        ScenarioCategory category = categoryRepository.findByCategoryKey(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        return new ScenarioListResponse(category.getCategoryKey(), scenarioRepository.findByCategoryOrderByIdAsc(category)
                .stream()
                .map(scenario -> new ScenarioListItemResponse(
                        scenario.getScenarioKey(),
                        scenario.getTitle(),
                        scenario.getDifficulty(),
                        scenario.getSuccessGoal(),
                        scenario.getThumbnailUrl()))
                .toList());
    }

    public ScenarioDetailResponse getScenarioDetail(String scenarioId) {
        Scenario scenario = scenarioRepository.findByScenarioKey(scenarioId)
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));

        return new ScenarioDetailResponse(
                scenario.getScenarioKey(),
                scenario.getCategory().getCategoryKey(),
                scenario.getTitle(),
                scenario.getDifficulty(),
                scenario.getSituationDescription(),
                scenario.getSuccessGoal(),
                scenario.getOpeningBabsaeText(),
                scenario.getOpeningTtsUrl(),
                scenario.getMaxFollowUpCount());
    }
}
