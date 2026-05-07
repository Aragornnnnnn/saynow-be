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

import java.time.LocalDateTime;

@Entity
@Table(name = "session_prompts")
public class SessionPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PracticeSession session;

    @Column(name = "prompt_index", nullable = false)
    private int promptIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_type", nullable = false, length = 30)
    private PromptType promptType;

    @Column(name = "prompt_text", nullable = false, length = 500)
    private String promptText;

    @Column(name = "tts_url", length = 500)
    private String ttsUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected SessionPrompt() {
    }

    public SessionPrompt(PracticeSession session, int promptIndex, PromptType promptType, String promptText, String ttsUrl, LocalDateTime createdAt) {
        this.session = session;
        this.promptIndex = promptIndex;
        this.promptType = promptType;
        this.promptText = promptText;
        this.ttsUrl = ttsUrl;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public PracticeSession getSession() {
        return session;
    }

    public int getPromptIndex() {
        return promptIndex;
    }

    public PromptType getPromptType() {
        return promptType;
    }

    public String getPromptText() {
        return promptText;
    }

    public String getTtsUrl() {
        return ttsUrl;
    }
}
