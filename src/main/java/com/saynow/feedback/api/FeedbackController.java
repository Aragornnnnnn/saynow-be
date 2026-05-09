package com.saynow.feedback.api;

import com.saynow.auth.security.AuthMemberPrincipal;
import com.saynow.common.response.ApiResponse;
import com.saynow.feedback.api.dto.FeedbackResponse;
import com.saynow.feedback.application.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "MVP 세션 피드백 API")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping("/{sessionId}/feedback")
    @Operation(summary = "세션 피드백 조회", description = "완료된 세션의 종합 피드백과 턴별 피드백을 조회합니다.")
    public ApiResponse<FeedbackResponse> getFeedback(
            @AuthenticationPrincipal AuthMemberPrincipal principal,
            @PathVariable String sessionId
    ) {
        return ApiResponse.success(feedbackService.getFeedback(principal.memberId(), sessionId));
    }
}
