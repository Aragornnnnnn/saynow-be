package com.saynow.practice.infrastructure.ai;

import com.saynow.practice.domain.PromptType;

public record AiPrompt(
        PromptType promptType,
        String promptText,
        String ttsUrl
) {
}
