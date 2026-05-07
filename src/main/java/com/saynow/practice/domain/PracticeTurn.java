package com.saynow.practice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "practice_turns")
public class PracticeTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prompt_id", nullable = false)
    private SessionPrompt prompt;

    @Column(name = "turn_index", nullable = false)
    private int turnIndex;

    @Column(name = "user_transcript", nullable = false, columnDefinition = "text")
    private String userTranscript;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private InputType inputType;

    @Column(name = "speech_started_after_ms")
    private Integer speechStartedAfterMs;

    @Column(name = "recording_duration_ms")
    private Integer recordingDurationMs;

    @Column(name = "stt_confidence", precision = 5, scale = 4)
    private BigDecimal sttConfidence;

    @Column(name = "audio_object_key", length = 500)
    private String audioObjectKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PracticeTurn() {
    }

    public PracticeTurn(
            PracticeSession session,
            SessionPrompt prompt,
            int turnIndex,
            String userTranscript,
            InputType inputType,
            Integer speechStartedAfterMs,
            Integer recordingDurationMs,
            BigDecimal sttConfidence,
            LocalDateTime createdAt
    ) {
        this.session = session;
        this.prompt = prompt;
        this.turnIndex = turnIndex;
        this.userTranscript = userTranscript;
        this.inputType = inputType;
        this.speechStartedAfterMs = speechStartedAfterMs;
        this.recordingDurationMs = recordingDurationMs;
        this.sttConfidence = sttConfidence;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public SessionPrompt getPrompt() {
        return prompt;
    }

    public int getTurnIndex() {
        return turnIndex;
    }

    public String getUserTranscript() {
        return userTranscript;
    }

    public Integer getSpeechStartedAfterMs() {
        return speechStartedAfterMs;
    }

    public Integer getRecordingDurationMs() {
        return recordingDurationMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
