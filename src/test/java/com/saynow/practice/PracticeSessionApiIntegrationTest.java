package com.saynow.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PracticeSessionApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void completesSessionAndReturnsFeedbackWithoutExposingInternalSlots() throws Exception {
        String sessionId = startSession("cafe_iced_americano");

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/metrics/micReady", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":1240}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.micReadyLatencyMs").value(1240));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transcript": "I want iced americano",
                                  "inputType": "AUDIO",
                                  "speechStartedAfterMs": 2100,
                                  "recordingDurationMs": 3600,
                                  "sttConfidence": 0.86
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.turnId").value(1))
                .andExpect(jsonPath("$.turnIndex").value(1))
                .andExpect(jsonPath("$.transcript").value("I want iced americano"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.babsaeText").value("What size would you like?"))
                .andExpect(jsonPath("$.followUpCount").value(1))
                .andExpect(jsonPath("$.maxFollowUpCount").value(5))
                .andExpect(jsonPath("$.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.missingSlots").doesNotExist());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "transcript": "Small for here, please.",
                                  "inputType": "TEXT",
                                  "speechStartedAfterMs": 1200,
                                  "recordingDurationMs": 1600
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnId").value(2))
                .andExpect(jsonPath("$.turnIndex").value(2))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.babsaeText").value("Scenario cleared."))
                .andExpect(jsonPath("$.followUpCount").value(1))
                .andExpect(jsonPath("$.feedbackAvailable").value(true))
                .andExpect(jsonPath("$.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.missingSlots").doesNotExist());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.babsaeText").value("Scenario cleared."))
                .andExpect(jsonPath("$.micReadyLatencyMs").value(1240))
                .andExpect(jsonPath("$.turns", hasSize(2)))
                .andExpect(jsonPath("$.turns[0].questionText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.turns[0].userTranscript").value("I want iced americano"));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/feedback", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.scenarioResult").value("SUCCESS"))
                .andExpect(jsonPath("$.totalUnderstoodScore").value(85))
                .andExpect(jsonPath("$.turnFeedback", hasSize(2)))
                .andExpect(jsonPath("$.turnFeedback[0].turnId").value(1))
                .andExpect(jsonPath("$.turnFeedback[0].questionText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.turnFeedback[0].speechStartedAfterSeconds").value(2.1))
                .andExpect(jsonPath("$.turnFeedback[0].heardAs").isNotEmpty());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"transcript":"Another answer","inputType":"TEXT"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ENDED"));
    }

    @Test
    void exitsSessionAndRejectsFurtherUpdates() throws Exception {
        String sessionId = startSession("taxi_destination");

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/exit", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"USER_EXIT"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.status").value("ABANDONED"))
                .andExpect(jsonPath("$.endedAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/metrics/micReady", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":400}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ENDED"));
    }

    private String startSession(String scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"%s"}
                                """.formatted(scenarioId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.babsaeText").isNotEmpty())
                .andExpect(jsonPath("$.followUpCount").value(0))
                .andExpect(jsonPath("$.maxFollowUpCount").value(5))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("sessionId").asText();
    }
}
