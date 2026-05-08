package com.saynow.practice.api;

import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.response.ApiResponse;
import com.saynow.practice.api.dto.ExitSessionResponse;
import com.saynow.practice.api.dto.MicReadyRequest;
import com.saynow.practice.api.dto.MicReadyResponse;
import com.saynow.practice.api.dto.SessionStartResponse;
import com.saynow.practice.api.dto.SessionStatusResponse;
import com.saynow.practice.api.dto.StartSessionRequest;
import com.saynow.practice.api.dto.SubmitTurnRequest;
import com.saynow.practice.api.dto.TurnSubmitResponse;
import com.saynow.practice.application.PracticeSessionService;
import com.saynow.practice.application.SubmittedAudio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/sessions")
@Validated
@Tag(name = "Practice Session", description = "MVP 회화 세션 진행 API")
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;

    public PracticeSessionController(PracticeSessionService practiceSessionService) {
        this.practiceSessionService = practiceSessionService;
    }

    @PostMapping
    @Operation(summary = "세션 시작", description = "시나리오를 선택해 연습 세션을 시작하고 첫 질문을 반환합니다.")
    public ResponseEntity<ApiResponse<SessionStartResponse>> startSession(@Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(practiceSessionService.startSession(request)));
    }

    @GetMapping("/{sessionId}")
    @Operation(summary = "세션 상태 조회", description = "현재 세션 상태와 진행 턴 이력을 조회합니다.")
    public ApiResponse<SessionStatusResponse> getSession(@PathVariable String sessionId) {
        return ApiResponse.success(practiceSessionService.getSession(sessionId));
    }

    @PutMapping("/{sessionId}/micReady")
    @Operation(summary = "마이크 준비 지연 기록", description = "세션별 마이크 준비까지 걸린 시간을 기록합니다.")
    public ApiResponse<MicReadyResponse> recordMicReady(@PathVariable String sessionId, @Valid @RequestBody MicReadyRequest request) {
        return ApiResponse.success(practiceSessionService.recordMicReady(sessionId, request));
    }

    @PostMapping(value = "/{sessionId}/turns", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "턴 음성 제출", description = "사용자 음성 파일과 턴 메트릭을 제출하고 AI 평가 결과를 반환합니다.")
    public ApiResponse<TurnSubmitResponse> submitTurn(
            @PathVariable String sessionId,
            @Parameter(description = "사용자 발화 음성 파일")
            @RequestPart("audio") MultipartFile audio,
            @Valid @ModelAttribute SubmitTurnRequest request
    ) {
        return ApiResponse.success(practiceSessionService.submitTurn(sessionId, toSubmittedAudio(audio), request));
    }

    @PostMapping("/{sessionId}/exit")
    @Operation(summary = "세션 중도 종료", description = "진행 중인 세션을 사용자 종료 상태로 마감합니다.")
    public ApiResponse<ExitSessionResponse> exitSession(@PathVariable String sessionId) {
        return ApiResponse.success(practiceSessionService.exitSession(sessionId));
    }

    private SubmittedAudio toSubmittedAudio(MultipartFile audio) {
        try {
            return new SubmittedAudio(audio.getOriginalFilename(), audio.getContentType(), audio.getSize(), audio.getBytes());
        } catch (IOException exception) {
            throw new ApiException(ErrorCode.AUDIO_READ_FAILED);
        }
    }
}
