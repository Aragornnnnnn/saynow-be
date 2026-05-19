// 2차 MVP 홈 화면 시나리오 목록과 잠금 상태를 계산하는 서비스
package com.saynow.scenario.application;

import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.CategoryResponse;
import com.saynow.scenario.api.dto.ScenarioResponse;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.Category;
import com.saynow.scenario.domain.UserScenarioProgress;
import com.saynow.scenario.infrastructure.CategoryRepository;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import com.saynow.scenario.infrastructure.UserScenarioProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioService {

    public static final String COMING_SOON = "COMING_SOON";
    public static final String PREVIOUS_SCENARIO_NOT_CLEARED = "PREVIOUS_SCENARIO_NOT_CLEARED";
    private static final String OPEN_CATEGORY_NAME = "Cafe";

    private final CategoryRepository categoryRepository;
    private final ScenarioRepository scenarioRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;

    public CategoryListResponse getScenarios(Long userId) {
        List<Category> categories = categoryRepository.findAllByOrderByIdAsc();
        List<Scenario> scenarios = scenarioRepository.findAllByOrderByCategoryIdAscDisplayOrderAsc();
        Map<Long, Boolean> clearedByScenarioId = userScenarioProgressRepository.findByUserIdAndScenarioIdIn(
                        userId,
                        scenarios.stream().map(Scenario::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getScenario().getId(), UserScenarioProgress::isCleared));
        Map<Long, List<Scenario>> scenariosByCategoryId = scenarios.stream()
                .collect(Collectors.groupingBy(scenario -> scenario.getCategory().getId()));

        return new CategoryListResponse(categories.stream()
                .map(category -> toCategoryResponse(category, scenariosByCategoryId.getOrDefault(category.getId(), List.of()), clearedByScenarioId))
                .toList());
    }

    private CategoryResponse toCategoryResponse(
            Category category,
            List<Scenario> scenarios,
            Map<Long, Boolean> clearedByScenarioId
    ) {
        boolean categoryLocked = !OPEN_CATEGORY_NAME.equals(category.getName());
        if (categoryLocked) {
            return new CategoryResponse(category.getId(), category.getName(), true, COMING_SOON, List.of());
        }

        List<ScenarioResponse> scenarioResponses = new ArrayList<>();
        boolean previousCleared = true;
        for (Scenario scenario : scenarios) {
            boolean cleared = clearedByScenarioId.getOrDefault(scenario.getId(), false);
            boolean locked = !previousCleared;
            scenarioResponses.add(new ScenarioResponse(
                    scenario.getId(),
                    scenario.getDisplayOrder(),
                    scenario.getTitle(),
                    scenario.getGoal(),
                    scenario.getEmoji(),
                    cleared,
                    locked,
                    locked ? PREVIOUS_SCENARIO_NOT_CLEARED : null));
            previousCleared = cleared;
        }
        return new CategoryResponse(category.getId(), category.getName(), false, null, scenarioResponses);
    }
}
