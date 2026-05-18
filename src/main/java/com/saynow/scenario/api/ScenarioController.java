// 홈 화면의 카테고리별 시나리오 목록 API를 제공하는 컨트롤러
package com.saynow.scenario.api;

import com.saynow.auth.security.AuthUserPrincipal;
import com.saynow.common.response.ApiResponse;
import com.saynow.scenario.api.dto.CategoryListResponse;
import com.saynow.scenario.application.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Scenario", description = "2차 MVP 시나리오 탐색 API")
public class ScenarioController {

    private final ScenarioService scenarioService;

    @GetMapping("/scenarios")
    @Operation(summary = "시나리오 전체 조회", description = "카테고리별 시나리오 목록, 사용자별 클리어 여부, 잠금 여부를 조회합니다.")
    public ApiResponse<CategoryListResponse> getScenarios(@AuthenticationPrincipal AuthUserPrincipal principal) {
        return ApiResponse.success(scenarioService.getScenarios(principal.userId()));
    }
}
