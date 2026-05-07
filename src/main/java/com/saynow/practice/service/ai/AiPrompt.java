package com.saynow.practice.service.ai;

import com.saynow.practice.domain.PromptType;

public record AiPrompt(
        PromptType promptType,
        String promptText,
        String ttsUrl
) {
}
