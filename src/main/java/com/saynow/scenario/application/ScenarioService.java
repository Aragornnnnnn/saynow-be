package com.saynow.scenario.application;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.CategoryResponse;
import com.saynow.scenario.api.dto.ScenarioDetailResponse;
import com.saynow.scenario.api.dto.ScenarioSummaryListResponse;
import com.saynow.scenario.api.dto.ScenarioSummaryResponse;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioCategory;
import com.saynow.scenario.infrastructure.ScenarioCategoryRepository;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioService {

    private static final String ALL_CATEGORY_ID = "all";

    private final ScenarioCategoryRepository categoryRepository;
    private final ScenarioRepository scenarioRepository;

    public CategoryListResponse getCategories() {
        return new CategoryListResponse(categoryRepository.findAllByOrderByIdAsc()
                .stream()
                .map(category -> new CategoryResponse(category.getCategoryKey(), category.getName()))
                .toList());
    }

    public ScenarioSummaryListResponse getScenarios(String categoryId) {
        if (categoryId == null || ALL_CATEGORY_ID.equals(categoryId)) {
            return new ScenarioSummaryListResponse(scenarioRepository.findAllByOrderByIdAsc()
                    .stream()
                    .map(this::toSummaryResponse)
                    .toList());
        }

        ScenarioCategory category = categoryRepository.findByCategoryKey(categoryId)
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));

        return new ScenarioSummaryListResponse(scenarioRepository.findByCategoryOrderByIdAsc(category)
                .stream()
                .map(this::toSummaryResponse)
                .toList());
    }

    public ScenarioDetailResponse getScenarioDetail(String scenarioId) {
        Scenario scenario = scenarioRepository.findByScenarioKey(scenarioId)
                .orElseThrow(() -> new ApiException(ErrorCode.SCENARIO_NOT_FOUND));

        return new ScenarioDetailResponse(
                scenario.getScenarioKey(),
                scenario.getCategory().getCategoryKey(),
                scenario.getTitle(),
                scenario.getDifficulty().getDisplayName(),
                scenario.getSituationDescription(),
                scenario.getSuccessGoal(),
                scenario.getOpeningBabsaeText(),
                scenario.getOpeningTtsUrl(),
                scenario.getMaxFollowUpCount(),
                scenario.getThumbnailUrl());
    }

    private ScenarioSummaryResponse toSummaryResponse(Scenario scenario) {
        return new ScenarioSummaryResponse(
                scenario.getScenarioKey(),
                scenario.getCategory().getCategoryKey(),
                scenario.getTitle(),
                scenario.getDifficulty().getDisplayName(),
                scenario.getSuccessGoal(),
                scenario.getThumbnailUrl());
    }
}
