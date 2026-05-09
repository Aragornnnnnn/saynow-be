package com.saynow.practice.domain;

import com.saynow.common.domain.BaseTimeEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "practice_turns")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PracticeTurn extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @Column(name = "turn_index", nullable = false)
    @Getter
    private int turnIndex;

    @Column(name = "question_text", nullable = false, length = 500)
    @Getter
    private String questionText;

    @Column(name = "question_tts_url", length = 500)
    @Getter
    private String questionTtsUrl;

    @Column(name = "user_transcript", nullable = false, columnDefinition = "text")
    @Getter
    private String userTranscript;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private InputType inputType;

    @Column(name = "speech_started_after_ms")
    @Getter
    private Integer speechStartedAfterMs;

    @Column(name = "recording_duration_ms")
    @Getter
    private Integer recordingDurationMs;

    @Column(name = "stt_confidence", precision = 5, scale = 4)
    @Getter
    private BigDecimal sttConfidence;

    @Column(name = "audio_object_key", length = 500)
    private String audioObjectKey;

    public PracticeTurn(
            PracticeSession session,
            int turnIndex,
            String questionText,
            String questionTtsUrl,
            String userTranscript,
            InputType inputType,
            Integer speechStartedAfterMs,
            Integer recordingDurationMs,
            BigDecimal sttConfidence
    ) {
        this.session = session;
        this.turnIndex = turnIndex;
        this.questionText = questionText;
        this.questionTtsUrl = questionTtsUrl;
        this.userTranscript = userTranscript;
        this.inputType = inputType;
        this.speechStartedAfterMs = speechStartedAfterMs;
        this.recordingDurationMs = recordingDurationMs;
        this.sttConfidence = sttConfidence;
    }

}
