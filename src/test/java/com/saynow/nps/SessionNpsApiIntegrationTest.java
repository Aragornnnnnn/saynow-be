// 3차 MVP 세션 최종 피드백 이후 NPS 제출 정책을 통합 검증한다.
package com.saynow.nps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.session.infrastructure.ai.AiClosingMessageRequest;
import com.saynow.session.infrastructure.ai.AiClosingMessageResponse;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiSessionFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiSessionFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiSessionTurnFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackStatusResponse;
import com.saynow.session.domain.InnerThoughtType;
import com.saynow.session.infrastructure.ai.FeedbackType;
import com.saynow.session.infrastructure.ai.TurnFeedbackStatus;
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
    void submitNpsAfterFinalFeedbackReturnsCreatedWithNoDataAndPersists() throws Exception {
        String accessToken = login("nps-mvp3-sub-1|nps1@example.com|NPS User");
        long sessionId = startSession(accessToken);
        answerAllQuestions(accessToken, sessionId);
        createFeedback(accessToken, sessionId);

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
                SELECT r.session_id || '|' || r.score || '|' || r.low_score_reason
                FROM session_nps_responses r
                WHERE r.session_id = ?
                """, String.class, sessionId);
        assertThat(rows).containsExactly(
                sessionId + "|2|질문이 제 답변과 잘 이어지지 않는 느낌이 있었어요.");
    }

    @Test
    void submitNpsRejectsBeforeFinalFeedback() throws Exception {
        String accessToken = login("nps-mvp3-sub-2|nps2@example.com|NPS Progress User");
        long sessionId = startSession(accessToken);
        answerAllQuestions(accessToken, sessionId);

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
    void submitNpsRejectsDuplicateSubmissionBySession() throws Exception {
        String accessToken = login("nps-mvp3-sub-3|nps3@example.com|NPS Duplicate User");
        long sessionId = startSession(accessToken);
        answerAllQuestions(accessToken, sessionId);
        createFeedback(accessToken, sessionId);

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
    void submitNpsRejectsOtherUsersSession() throws Exception {
        String ownerAccessToken = login("nps-mvp3-sub-4|nps4@example.com|NPS Owner User");
        String otherAccessToken = login("nps-mvp3-sub-5|nps5@example.com|NPS Other User");
        long sessionId = startSession(ownerAccessToken);
        answerAllQuestions(ownerAccessToken, sessionId);
        createFeedback(ownerAccessToken, sessionId);

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
    void submitNpsRejectsScoreOutsideRange() throws Exception {
        String accessToken = login("nps-mvp3-sub-6|nps6@example.com|NPS Score User");
        long sessionId = startSession(accessToken);
        answerAllQuestions(accessToken, sessionId);
        createFeedback(accessToken, sessionId);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/nps", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"score":6}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    private long startSession(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
    }

    private void answerAllQuestions(String accessToken, long sessionId) throws Exception {
        for (String utterance : List.of(
                "I like pizza because it is spicy.",
                "I cook pasta on weekends.",
                "I ate ramen yesterday.",
                "I want to try tacos because they look fresh.")) {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"userUtterance":"%s"}
                                    """.formatted(utterance)))
                    .andExpect(status().isOk());
        }
    }

    private void createFeedback(String accessToken, long sessionId) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk());
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
            return new AiNextQuestionResponse(
                    request.nextQuestion().questionEn(),
                    request.nextQuestion().questionKo(),
                    "답변을 이어줘서 다음 질문으로 자연스럽게 넘어가면 좋겠다.",
                    InnerThoughtType.NORMAL);
        }

        @Override
        public AiClosingMessageResponse generateClosingMessage(AiClosingMessageRequest request) {
            return new AiClosingMessageResponse(
                    "Thanks for sharing. That was a good conversation.",
                    "이야기해줘서 고마워. 좋은 대화였어.",
                    "마지막까지 답해줘서 대화를 자연스럽게 마무리하면 좋겠다.",
                    InnerThoughtType.NORMAL);
        }

        @Override
        public AiTurnFeedbackStatusResponse generateTurnFeedback(AiTurnFeedbackRequest request) {
            return new AiTurnFeedbackStatusResponse(request.sessionId(), request.turnId(), TurnFeedbackStatus.PREPARING);
        }

        @Override
        public AiSessionFeedbackResponse generateSessionFeedback(AiSessionFeedbackRequest request) {
            return new AiSessionFeedbackResponse(
                    request.sessionId(),
                    82,
                    "한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요.",
                    request.expectedTurnIds().stream()
                            .map(turnId -> new AiSessionTurnFeedbackResponse(
                                    turnId,
                                    FeedbackType.GOOD,
                                    "한국어로 비유하자면 자연스럽게 들려요.",
                                    null,
                                    "답변 의도가 분명했기 때문이에요.",
                                    null,
                                    null,
                                    "한국인의 35%가 틀리는 표현인데 정확히 맞췄어요."))
                            .toList());
        }
    }
}
