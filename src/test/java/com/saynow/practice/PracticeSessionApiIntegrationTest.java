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

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/metrics/micReady", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":1240}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.micReadyLatencyMs").value(1240));

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.webm", "I want iced americano"))
                        .param("inputType", "AUDIO")
                        .param("speechStartedAfterMs", "2100")
                        .param("recordingDurationMs", "3600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(sessionId))
                .andExpect(jsonPath("$.turnId").value(1))
                .andExpect(jsonPath("$.turnIndex").value(1))
                .andExpect(jsonPath("$.transcript").value("I want iced americano"))
                .andExpect(jsonPath("$.sttConfidence").value(0.86))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.babsaeText").value("What size would you like?"))
                .andExpect(jsonPath("$.followUpCount").value(1))
                .andExpect(jsonPath("$.maxFollowUpCount").value(5))
                .andExpect(jsonPath("$.feedbackAvailable").value(false))
                .andExpect(jsonPath("$.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.missingSlots").doesNotExist());

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-2.webm", "Small for here, please."))
                        .param("inputType", "AUDIO")
                        .param("speechStartedAfterMs", "1200")
                        .param("recordingDurationMs", "1600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.turnId").value(2))
                .andExpect(jsonPath("$.turnIndex").value(2))
                .andExpect(jsonPath("$.transcript").value("Small for here, please."))
                .andExpect(jsonPath("$.sttConfidence").value(0.92))
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

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-3.webm", "Another answer"))
                        .param("inputType", "AUDIO"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_ENDED"));
    }

    @Test
    void rejectsTurnSubmitWithoutAudio() throws Exception {
        String sessionId = startSession("cafe_iced_americano");

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .param("inputType", "AUDIO")
                        .param("speechStartedAfterMs", "2100")
                        .param("recordingDurationMs", "3600"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsUnsupportedAudioType() throws Exception {
        String sessionId = startSession("cafe_iced_americano");

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(new MockMultipartFile("audio", "turn-1.txt", MediaType.TEXT_PLAIN_VALUE, "I want iced americano".getBytes()))
                        .param("inputType", "AUDIO"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_AUDIO_TYPE"));
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

    private MockMultipartFile audio(String filename, String transcript) {
        return new MockMultipartFile("audio", filename, "audio/webm", transcript.getBytes());
    }
}
