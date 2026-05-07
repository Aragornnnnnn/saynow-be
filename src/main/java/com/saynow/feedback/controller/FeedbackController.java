package com.saynow.feedback.controller;

import com.saynow.feedback.controller.dto.FeedbackResponse;
import com.saynow.feedback.service.FeedbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/{sessionId}/feedback")
    public FeedbackResponse getFeedback(@PathVariable String sessionId) {
        return feedbackService.getFeedback(sessionId);
    }
}
