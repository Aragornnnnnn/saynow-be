// 세션 NPS 평가 제출 API 동작을 통합 검증한다.
package com.saynow.nps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiFilledSlot;
import com.saynow.session.infrastructure.ai.AiGuideRequest;
import com.saynow.session.infrastructure.ai.AiGuideResponse;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiSlotStatus;
import com.saynow.session.infrastructure.ai.TurnClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionNpsApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestAiConversationClient aiConversationClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiConversationClient.reset();
    }

    @Test
    void submitNpsForCompletedSessionReturnsCreatedWithNoDataAndPersists() throws Exception {
        String accessToken = login("nps-sub-1|nps1@example.com|NPS User");
        long sessionId = startSession(accessToken);
        completeSession(accessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score": 2,
                                  "lowScoreReason": "질문이 제 답변과 잘 이어지지 않는 느낌이 있었어요."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error").value(nullValue()));

        List<String> rows = jdbcTemplate.queryForList("""
                SELECT u.email || '|' || r.session_id || '|' || r.score || '|' || r.low_score_reason
                FROM session_nps_responses r
                JOIN users u ON u.id = r.user_id
                WHERE r.session_id = ?
                """, String.class, sessionId);
        assertThat(rows).containsExactly(
                "nps1@example.com|" + sessionId + "|2|질문이 제 답변과 잘 이어지지 않는 느낌이 있었어요.");
    }

    @Test
    void submitNpsRejectsScoreOutsideRange() throws Exception {
        String accessToken = login("nps-sub-2|nps2@example.com|NPS Score User");
        long sessionId = startSession(accessToken);
        completeSession(accessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void submitNpsRejectsHighScoreReason() throws Exception {
        String accessToken = login("nps-sub-3|nps3@example.com|NPS High User");
        long sessionId = startSession(accessToken);
        completeSession(accessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "score": 5,
                                  "lowScoreReason": "좋았지만 의견을 보냅니다."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void submitNpsRejectsInProgressSession() throws Exception {
        String accessToken = login("nps-sub-4|nps4@example.com|NPS Progress User");
        long sessionId = startSession(accessToken);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":4}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SESSION_IN_PROGRESS"));
    }

    @Test
    void submitNpsRejectsOtherUsersSession() throws Exception {
        String ownerAccessToken = login("nps-sub-5|nps5@example.com|NPS Owner User");
        String otherAccessToken = login("nps-sub-6|nps6@example.com|NPS Other User");
        long sessionId = startSession(ownerAccessToken);
        completeSession(ownerAccessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":4}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void submitNpsRejectsDuplicateSubmission() throws Exception {
        String accessToken = login("nps-sub-7|nps7@example.com|NPS Duplicate User");
        long sessionId = startSession(accessToken);
        completeSession(accessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":4}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":3}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("NPS_ALREADY_SUBMITTED"));
    }

    @Test
    void submitNpsRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/sessions/1/nps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":4}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    private long startSession(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/4/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
    }

    private void completeSession(String accessToken, long sessionId) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'll stay for five days."}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'll stay at the Midtown Hotel."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedbackAvailable").value(true));
    }

    private String login(String idToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"%s",
                                  "nonce":"nonce"
                                }
                                """.formatted(idToken)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("accessToken").asText();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    @TestConfiguration
    static class SessionNpsTestConfiguration {

        @Bean
        @Primary
        TestAiConversationClient testAiConversationClient() {
            return new TestAiConversationClient();
        }
    }

    static class TestAiConversationClient implements AiConversationClient {

        void reset() {
        }

        @Override
        public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
            List<AiSlotStatus> slots = request.slots() == null ? List.of() : request.slots();
            List<AiFilledSlot> newlyFilled = new ArrayList<>();
            for (AiSlotStatus slot : slots) {
                if (!slot.filled()) {
                    newlyFilled.add(new AiFilledSlot(slot.slotName()));
                    break;
                }
            }

            long remainingAfterFill = slots.stream()
                    .filter(slot -> !slot.filled())
                    .count() - newlyFilled.size();
            if (remainingAfterFill <= 0) {
                return new AiNextQuestionResponse(null, null, newlyFilled, TurnClassification.ANSWER);
            }

            String nextSlot = slots.stream()
                    .filter(slot -> !slot.filled())
                    .map(AiSlotStatus::slotName)
                    .filter(slotName -> newlyFilled.stream().noneMatch(filled -> filled.slotName().equals(slotName)))
                    .findFirst()
                    .orElse("detail");
            return new AiNextQuestionResponse(
                    "Could you tell me your " + nextSlot + "?",
                    nextSlot + "에 대해 말해주시겠어요?",
                    newlyFilled,
                    TurnClassification.ANSWER);
        }

        @Override
        public AiFeedbackResponse generateFeedback(AiFeedbackRequest request) {
            return new AiFeedbackResponse(80, "피드백 요약입니다.", List.of());
        }

        @Override
        public AiGuideResponse generateGuide(AiGuideRequest request) {
            return new AiGuideResponse("가이드 답변입니다.");
        }
    }
}
