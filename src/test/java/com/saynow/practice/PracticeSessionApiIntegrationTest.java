package com.saynow.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

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
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/micReady", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":1240}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.micReadyLatencyMs").value(1240));

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.webm", "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 2100, 3600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.turnId").exists())
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
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1200, 1600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.turnId").exists())
                .andExpect(jsonPath("$.data.turnIndex").value(2))
                .andExpect(jsonPath("$.data.transcript").value("Small for here, please."))
                .andExpect(jsonPath("$.data.sttConfidence").value(0.92))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.babsaeText").value("Scenario cleared."))
                .andExpect(jsonPath("$.data.followUpCount").value(1))
                .andExpect(jsonPath("$.data.feedbackAvailable").value(true))
                .andExpect(jsonPath("$.data.filledSlots").doesNotExist())
                .andExpect(jsonPath("$.data.missingSlots").doesNotExist());

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.scenarioId").value("cafe_iced_americano"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.babsaeText").value("Scenario cleared."))
                .andExpect(jsonPath("$.data.micReadyLatencyMs").value(1240))
                .andExpect(jsonPath("$.data.turns", hasSize(2)))
                .andExpect(jsonPath("$.data.turns[0].questionText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.data.turns[0].userTranscript").value("I want iced americano"));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.scenarioResult").value("SUCCESS"))
                .andExpect(jsonPath("$.data.totalUnderstoodScore").value(85))
                .andExpect(jsonPath("$.data.averageScoreDelta").doesNotExist())
                .andExpect(jsonPath("$.data.turnFeedback", hasSize(2)))
                .andExpect(jsonPath("$.data.turnFeedback[0].turnId").exists())
                .andExpect(jsonPath("$.data.turnFeedback[0].questionText").value("Hi! What would you like to order?"))
                .andExpect(jsonPath("$.data.turnFeedback[0].speechStartedAfterSeconds").value(2.1))
                .andExpect(jsonPath("$.data.turnFeedback[0].heardAs").isNotEmpty());

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-3.webm", "Another answer"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_ENDED"));
    }

    @Test
    void rejectsTurnSubmitWithoutAudio() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 2100, 3600)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void submitsTurnWithSwaggerMultipartRequestPart() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.webm", "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.turnIndex").value(1))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsTurnRequestPartWithoutJsonContentType() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("output.mp3", MediaType.APPLICATION_OCTET_STREAM_VALUE, "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000, MediaType.TEXT_PLAIN_VALUE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void rejectsInvalidTurnRequestPartJson() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("output.mp3", MediaType.APPLICATION_OCTET_STREAM_VALUE, "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(new MockMultipartFile("request", "", MediaType.TEXT_PLAIN_VALUE, "{".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    void acceptsMp3AudioContentTypeAlias() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.mp3", "audio/mp3", "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsM4aAudioContentTypeAlias() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.m4a", "audio/m4a", "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsWebmVideoContentTypeAlias() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.webm", "video/webm", "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsWebmAudioContentTypeWithCodecParameter() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.webm", "audio/webm;codecs=opus", "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsMp3UploadWhenContentTypeIsGeneric() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("output.mp3", MediaType.APPLICATION_OCTET_STREAM_VALUE, "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsExplicitUnsupportedContentTypeWhenAudioPayloadExists() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(audio("turn-1.mp3", MediaType.TEXT_PLAIN_VALUE, "I want iced americano"))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void acceptsUploadedFileWithoutAudioContentTypeValidation() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("cafe_iced_americano", accessToken);

        mockMvc.perform(multipart("/api/v1/sessions/{sessionId}/turns", sessionId)
                        .file(new MockMultipartFile("audio", "turn-1.txt", MediaType.TEXT_PLAIN_VALUE, "I want iced americano".getBytes()))
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .file(turnRequest("AUDIO", 1000, 1000)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.transcript").value("I want iced americano"));
    }

    @Test
    void exitsSessionAndRejectsFurtherUpdates() throws Exception {
        String accessToken = loginAccessToken();
        String sessionId = startSession("taxi_destination", accessToken);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/exit", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.status").value("ABANDONED"))
                .andExpect(jsonPath("$.data.endedAt").isNotEmpty());

        mockMvc.perform(put("/api/v1/sessions/{sessionId}/micReady", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"latencyMs":400}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_ALREADY_ENDED"));
    }

    private String startSession(String scenarioId, String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
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

    private String loginAccessToken() throws Exception {
        String subject = UUID.randomUUID().toString();
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s|practice@example.com|Practice User",
                                  "nonce":"nonce"
                                }
                                """.formatted(subject)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("accessToken").asText();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private MockMultipartFile audio(String filename, String transcript) {
        return audio(filename, "audio/webm", transcript);
    }

    private MockMultipartFile audio(String filename, String contentType, String transcript) {
        return new MockMultipartFile("audio", filename, contentType, transcript.getBytes());
    }

    private MockMultipartFile turnRequest(String inputType, int speechStartedAfterMs, int recordingDurationMs) {
        return turnRequest(inputType, speechStartedAfterMs, recordingDurationMs, MediaType.APPLICATION_JSON_VALUE);
    }

    private MockMultipartFile turnRequest(String inputType, int speechStartedAfterMs, int recordingDurationMs, String contentType) {
        return new MockMultipartFile(
                "request",
                "",
                contentType,
                """
                        {
                          "inputType":"%s",
                          "speechStartedAfterMs":%d,
                          "recordingDurationMs":%d
                        }
                        """.formatted(inputType, speechStartedAfterMs, recordingDurationMs)
                        .getBytes(StandardCharsets.UTF_8));
    }
}
