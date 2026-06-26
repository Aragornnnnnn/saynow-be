// 3차 MVP 홈 화면 프리톡 시나리오 목록과 잠금 상태를 계산하는 서비스
package com.saynow.scenario.application;

import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.api.dto.CategoryResponse;
import com.saynow.scenario.api.dto.FirstQuestionPreviewResponse;
import com.saynow.scenario.api.dto.ScenarioResponse;
import com.saynow.scenario.domain.Category;
import com.saynow.scenario.domain.Scenario;
import com.saynow.scenario.domain.ScenarioQuestion;
import com.saynow.scenario.domain.UserScenarioProgress;
import com.saynow.scenario.infrastructure.CategoryRepository;
import com.saynow.scenario.infrastructure.ScenarioQuestionRepository;
import com.saynow.scenario.infrastructure.ScenarioRepository;
import com.saynow.scenario.infrastructure.UserScenarioProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScenarioService {

    public static final String COMING_SOON = "COMING_SOON";
    public static final String PREVIOUS_SCENARIO_NOT_COMPLETED = "PREVIOUS_SCENARIO_NOT_COMPLETED";

    private final CategoryRepository categoryRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioQuestionRepository scenarioQuestionRepository;
    private final UserScenarioProgressRepository userScenarioProgressRepository;

    public CategoryListResponse getScenarios(Long userId) {
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        List<Scenario> scenarios = scenarioRepository.findAllByOrderByCategoryIdAscDisplayOrderAsc();
        Map<Long, Boolean> completedByScenarioId = userScenarioProgressRepository.findByUserIdAndScenarioIdIn(
                        userId,
                        scenarios.stream().map(Scenario::getId).toList())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getScenario().getId(), UserScenarioProgress::isCompleted));
        Map<Long, List<Scenario>> scenariosByCategoryId = scenarios.stream()
                .collect(Collectors.groupingBy(scenario -> scenario.getCategory().getId(), LinkedHashMap::new, Collectors.toList()));
        Map<Long, ScenarioQuestion> firstQuestionByScenarioId = scenarioQuestionRepository.findFirstQuestionsByScenarioIn(scenarios)
                .stream()
                .collect(Collectors.toMap(
                        question -> question.getScenario().getId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));

        return new CategoryListResponse(categories.stream()
                .map(category -> toCategoryResponse(
                        category,
                        scenariosByCategoryId.getOrDefault(category.getId(), List.of()),
                        completedByScenarioId,
                        firstQuestionByScenarioId))
                .toList());
    }

    private CategoryResponse toCategoryResponse(
            Category category,
            List<Scenario> scenarios,
            Map<Long, Boolean> completedByScenarioId,
            Map<Long, ScenarioQuestion> firstQuestionByScenarioId
    ) {
        if (category.isLocked()) {
            return new CategoryResponse(category.getId(), category.getName(), true, category.getLockReason(), List.of());
        }

        List<ScenarioResponse> scenarioResponses = new ArrayList<>();
        boolean previousCompleted = true;
        for (Scenario scenario : scenarios) {
            boolean completed = completedByScenarioId.getOrDefault(scenario.getId(), false);
            boolean locked = scenario.isLocked() || !previousCompleted;
            String lockReason = scenario.isLocked()
                    ? scenario.getLockReason()
                    : locked ? PREVIOUS_SCENARIO_NOT_COMPLETED : null;
            scenarioResponses.add(new ScenarioResponse(
                    scenario.getId(),
                    scenario.getDisplayOrder(),
                    scenario.getTitle(),
                    scenario.getBriefing(),
                    scenario.getConversationGoal(),
                    completed,
                    locked,
                    lockReason,
                    locked ? null : toFirstQuestionPreview(firstQuestionByScenarioId.get(scenario.getId()))));
            previousCompleted = completed;
        }
        return new CategoryResponse(category.getId(), category.getName(), false, null, scenarioResponses);
    }

    private FirstQuestionPreviewResponse toFirstQuestionPreview(ScenarioQuestion question) {
        if (question == null) {
            return null;
        }
        return new FirstQuestionPreviewResponse(
                question.getId(),
                question.getQuestionEn(),
                question.getQuestionKo());
    }
}
