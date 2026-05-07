package com.saynow.practice.api;

import com.saynow.practice.api.dto.ExitSessionRequest;
import com.saynow.practice.api.dto.ExitSessionResponse;
import com.saynow.practice.api.dto.MicReadyRequest;
import com.saynow.practice.api.dto.MicReadyResponse;
import com.saynow.practice.api.dto.SessionStartResponse;
import com.saynow.practice.api.dto.SessionStatusResponse;
import com.saynow.practice.api.dto.StartSessionRequest;
import com.saynow.practice.api.dto.SubmitTurnRequest;
import com.saynow.practice.api.dto.TurnSubmitResponse;
import com.saynow.practice.application.PracticeSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class PracticeSessionController {

    private final PracticeSessionService practiceSessionService;

    public PracticeSessionController(PracticeSessionService practiceSessionService) {
        this.practiceSessionService = practiceSessionService;
    }

    @PostMapping
    public ResponseEntity<SessionStartResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(practiceSessionService.startSession(request));
    }

    @GetMapping("/{sessionId}")
    public SessionStatusResponse getSession(@PathVariable String sessionId) {
        return practiceSessionService.getSession(sessionId);
    }

    @PutMapping("/{sessionId}/metrics/micReady")
    public MicReadyResponse recordMicReady(@PathVariable String sessionId, @Valid @RequestBody MicReadyRequest request) {
        return practiceSessionService.recordMicReady(sessionId, request);
    }

    @PostMapping("/{sessionId}/turns")
    public TurnSubmitResponse submitTurn(@PathVariable String sessionId, @Valid @RequestBody SubmitTurnRequest request) {
        return practiceSessionService.submitTurn(sessionId, request);
    }

    @PostMapping("/{sessionId}/exit")
    public ExitSessionResponse exitSession(@PathVariable String sessionId, @Valid @RequestBody ExitSessionRequest request) {
        return practiceSessionService.exitSession(sessionId, request);
    }
}
