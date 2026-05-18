// 2차 MVP 시나리오 세션 진행 API를 제공하는 컨트롤러
package com.saynow.session.api;

import com.saynow.auth.security.AuthUserPrincipal;
import com.saynow.common.response.ApiResponse;
import com.saynow.session.api.dto.SessionStartResponse;
import com.saynow.session.api.dto.UserUtteranceRequest;
import com.saynow.session.api.dto.UserUtteranceResponse;
import com.saynow.session.application.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Session", description = "2차 MVP 시나리오 세션 진행 API")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/scenarios/{scenarioId}/sessions")
    @Operation(summary = "시나리오 세션 시작", description = "사용자가 시나리오를 선택해 새 대화 세션을 시작합니다.")
    public ResponseEntity<ApiResponse<SessionStartResponse>> startSession(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long scenarioId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(sessionService.startSession(principal.userId(), scenarioId)));
    }

    @PostMapping("/sessions/{sessionId}/utterances")
    @Operation(summary = "세션 사용자 발화 제출", description = "사용자가 AI 질문에 대한 발화를 제출합니다.")
    public ApiResponse<UserUtteranceResponse> submitUtterance(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId,
            @Valid @RequestBody UserUtteranceRequest request
    ) {
        return ApiResponse.success(sessionService.submitUtterance(principal.userId(), sessionId, request));
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "세션 중도 종료", description = "사용자가 진행 중인 세션을 중도 종료하고 해당 세션 데이터를 삭제합니다.")
    public ApiResponse<Void> deleteSession(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        sessionService.deleteSession(principal.userId(), sessionId);
        return ApiResponse.success(null);
    }
}
