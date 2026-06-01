// 세션 NPS 평가 제출 API를 제공하는 컨트롤러
package com.saynow.nps.api;

import com.saynow.auth.security.AuthUserPrincipal;
import com.saynow.common.response.ApiResponse;
import com.saynow.nps.api.dto.NpsSubmitRequest;
import com.saynow.nps.application.NpsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "NPS", description = "세션 NPS 평가 수집 API")
public class NpsController {

    private final NpsService npsService;

    @PostMapping("/{sessionId}/nps")
    @Operation(summary = "세션 NPS 평가 제출", description = "사용자가 완료한 특정 세션에 대해 1~5점 평가를 제출합니다.")
    public ResponseEntity<ApiResponse<Void>> submitNps(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId,
            @RequestBody NpsSubmitRequest request
    ) {
        npsService.submitNps(principal.userId(), sessionId, request);
        return ApiResponse.success(HttpStatus.CREATED, null);
    }
}
