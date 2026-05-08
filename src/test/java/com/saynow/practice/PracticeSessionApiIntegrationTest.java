package com.saynow.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/micReady", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":1240}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.micReadyLatencyMs").value(1240));

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.webm", "I want iced americano"))
                        .param("inputType", "AUDIO")
                        .param("speechStartedAfterMs", "2100")
                        .param("recordingDurationMs", "3600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.turnId").value(1))
                .andExpect(jsonPath("$.data.turnIndex").value(1))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"))
                .andExpect(jsonPath("$.data.sttConfidence").value(0.86))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.babsaeText").value("What size would you like?"))
                .andExpect(jsonPath("$.data.followUpCount").value(1))
                .andExpect(jsonPath("$.data.maxFollowUpCount").value(5))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.data.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.missingSlots").doesNotExist());

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-2.webm", "Small for here, please."))
                        .param("inputType", "AUDIO")
                        .param("speechStartedAfterMs", "1200")
                        .param("recordingDurationMs", "1600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turnId").value(2))
                .andExpect(jsonPath("$.data.turnIndex").value(2))
                .andExpect(jsonPath("$.data.transcript").value("Small for here, please."))
                .andExpect(jsonPath("$.data.sttConfidence").value(0.92))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.babsaeText").value("Scenario cleared."))
                .andExpect(jsonPath("$.data.followUpCount").value(1))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(true))
                .andExpect(jsonPath("$.data.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.missingSlots").doesNotExist());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.babsaeText").value("Scenario cleared."))
                .andExpect(jsonPath("$.data.micReadyLatencyMs").value(1240))
                .andExpect(jsonPath("$.data.turns", hasSize(2)))
                .andExpect(jsonPath("$.data.turns[0].questionText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.data.turns[0].userTranscript").value("I want iced americano"));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/feedback", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.scenarioResult").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalUnderstoodScore").value(85))
                .andExpect(jsonPath("$.data.averageScoreDelta").doesNotExist())
                .andExpect(jsonPath("$.data.turnFeedback", hasSize(2)))
                .andExpect(jsonPath("$.data.turnFeedback[0].turnId").value(1))
                .andExpect(jsonPath("$.data.turnFeedback[0].questionText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.data.turnFeedback[0].speechStartedAfterSeconds").value(2.1))
                .andExpect(jsonPath("$.data.turnFeedback[0].heardAs").isNotEmpty());

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-3.webm", "Another answer"))
                        .param("inputType", "AUDIO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_ENDED"));
    }

    @Test
    void rejectsTurnSubmitWithoutAudio() throws Exception {
        String sessionId = startSession("cafe_iced_americano");

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .param("inputType", "AUDIO")
                        .param("speechStartedAfterMs", "2100")
                        .param("recordingDurationMs", "3600"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void rejectsUnsupportedAudioType() throws Exception {
        String sessionId = startSession("cafe_iced_americano");

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(new MockMultipartFile("audio", "turn-1.txt", MediaType.TEXT_PLAIN_VALUE, "I want iced americano".getBytes()))
                        .param("inputType", "AUDIO"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_AUDIO_TYPE"));
    }

    @Test
    void exitsSessionAndRejectsFurtherUpdates() throws Exception {
        String sessionId = startSession("taxi_destination");

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/exit", sessionId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("ABANDONED"))
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/micReady", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":400}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_ENDED"));
    }

    private String startSession(String scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"%s"}
                                """.formatted(scenarioId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.babsaeText").isNotEmpty())
                .andExpect(jsonPath("$.data.followUpCount").value(0))
                .andExpect(jsonPath("$.data.maxFollowUpCount").value(5))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asText();
    }

    private MockMultipartFile audio(String filename, String transcript) {
        return new MockMultipartFile("audio", filename, "audio/webm", transcript.getBytes());
    }
}
