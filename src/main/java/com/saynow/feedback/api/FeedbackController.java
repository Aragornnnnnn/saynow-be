// 완료된 세션의 최종 피드백 생성 API를 제공하는 컨트롤러
package com.saynow.feedback.api;

import com.saynow.auth.security.AuthUserPrincipal;
import com.saynow.common.response.ApiResponse;
import com.saynow.feedback.api.dto.FeedbackResponse;
import com.saynow.feedback.application.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "2차 MVP 세션 피드백 API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/{sessionId}/feedback")
    @Operation(summary = "세션 완료 및 최종 피드백 생성", description = "사용자의 대화 세션을 완료 처리하고 전체 피드백과 턴별 피드백을 반환합니다.")
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        return ApiResponse.success(HttpStatus.OK, feedbackService.createFeedback(principal.userId(), sessionId));
    }

    @PostMapping(value = "/{sessionId}/feedback/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "세션 최종 피드백 스트림 생성", description = "AI 서버의 최종 피드백 SSE 이벤트를 프론트로 중계합니다.")
    public ResponseEntity<StreamingResponseBody> streamFeedback(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(feedbackService.streamFeedback(principal.userId(), sessionId));
    }
}
