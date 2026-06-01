// 3차 MVP 프리톡 시나리오 목록, 세션 진행, 최종 피드백 흐름을 통합 검증한다.
package com.saynow.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiSessionFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiSessionFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiSessionTurnFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackStatusResponse;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ScenarioFlowIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestAiConversationClient aiConversationClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiConversationClient.reset();
    }

    @Test
    void freeTalkScenarioCompletesAfterFourAnswersAndFinalFeedbackUnlocksNextScenario() throws Exception {
        String accessToken = login("mvp3-sub-1|mvp3@example.com|MVP3 User");

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("Free Talk"))
                .andExpect(jsonPath("$.data.categories[0].categoryLocked").value(false))
                .andExpect(jsonPath("$.data.categories[0].categoryLockReason").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].scenarioTitle").value("음식 취향 이야기하기"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].briefing").isString())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].conversationGoal").isString())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].locked").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.questionId").isNumber())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.aiQuestion")
                        .value("What is your favorite food? Why do you like it?"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.translatedQuestion")
                        .value("가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].lockReason").value("PREVIOUS_SCENARIO_NOT_COMPLETED"))
                .andExpect(jsonPath("$.data.categories[1].categoryName").value("Airport"))
                .andExpect(jsonPath("$.data.categories[1].categoryLocked").value(true))
                .andExpect(jsonPath("$.data.categories[1].categoryLockReason").value("COMING_SOON"));

        long sessionId = startSession(accessToken, 1);

        submitAndExpectNext(
                accessToken,
                sessionId,
                "I like pizza because it is spicy.",
                1,
                2,
                "That sounds tasty. Do you cook often?",
                "맛있겠네요. 요리는 자주 하나요?");
        submitAndExpectNext(
                accessToken,
                sessionId,
                "I cook pasta on weekends.",
                2,
                3,
                "Nice, weekends are a good time to cook. What food did you eat recently?",
                "좋아요, 주말은 요리하기 좋죠. 최근에 먹은 음식은 뭐였나요?");
        submitAndExpectNext(
                accessToken,
                sessionId,
                "I ate ramen yesterday.",
                3,
                4,
                "Ramen is a familiar comfort food. Is there a food you want to try next?",
                "라면은 익숙한 편안한 음식이죠. 다음에 먹어보고 싶은 음식이 있나요?");

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I want to try tacos because they look fresh."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedTurn.sequence").value(4))
                .andExpect(jsonPath("$.data.submittedTurn.turnFeedbackStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.nextTurn").value(nullValue()))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(4))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(true));

        assertThat(aiConversationClient.nextQuestionRequests).hasSize(3);
        assertThat(aiConversationClient.turnFeedbackRequests).hasSize(4);
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().currentTurn().userUtterance())
                .isEqualTo("I like pizza because it is spicy.");
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().nextQuestion().sequence())
                .isEqualTo(2);

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(true));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.nativeScore").value(82))
                .andExpect(jsonPath("$.data.nativeLevelLabel").value("유학생 수준"))
                .andExpect(jsonPath("$.data.summary").value("하고 싶은 말을 끝까지 전달했고 이유를 붙이는 힘이 좋았어요."))
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(4))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].sequence").value(1))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].feedbackType").value("GOOD"))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].koreanAnalogy").isString())
                .andExpect(jsonPath("$.data.turnFeedbacks[1].feedbackType").value("NEEDS_IMPROVEMENT"))
                .andExpect(jsonPath("$.data.turnFeedbacks[1].plusOneExpression").isString());

        assertThat(aiConversationClient.sessionFeedbackRequest.expectedTurnIds()).hasSize(4);
        Integer sessionFeedbackCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM session_feedbacks
                WHERE session_id = ?
                  AND native_score = 82
                  AND native_level_label = '유학생 수준'
                """, Integer.class, sessionId);
        assertThat(sessionFeedbackCount).isEqualTo(1);

        Integer turnFeedbackCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM turn_feedbacks tf
                JOIN session_turns st ON st.id = tf.turn_id
                WHERE st.session_id = ?
                """, Integer.class, sessionId);
        assertThat(turnFeedbackCount).isEqualTo(4);

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(false));
    }

    @Test
    void feedbackRequiresAllFourAnswers() throws Exception {
        String accessToken = login("mvp3-sub-2|feedback-not-ready@example.com|Not Ready User");
        long sessionId = startSession(accessToken, 1);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_COMPLETED"));
    }

    @Test
    void abandonMarksInProgressSessionAsAbandoned() throws Exception {
        String accessToken = login("mvp3-sub-3|abandon@example.com|Abandon User");
        long sessionId = startSession(accessToken, 1);

        mockMvc.perform(patch("/api/v1/sessions/{sessionId}/abandon", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        String status = jdbcTemplate.queryForObject("""
                SELECT status
                FROM sessions
                WHERE id = ?
                """, String.class, sessionId);
        assertThat(status).isEqualTo("ABANDONED");
    }

    private void submitAndExpectNext(
            String accessToken,
            long sessionId,
            String userUtterance,
            int submittedSequence,
            int nextSequence,
            String nextQuestion,
            String translatedQuestion
    ) throws Exception {
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"%s"}
                                """.formatted(userUtterance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedTurn.sequence").value(submittedSequence))
                .andExpect(jsonPath("$.data.submittedTurn.turnFeedbackStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.nextTurn.sequence").value(nextSequence))
                .andExpect(jsonPath("$.data.nextTurn.aiQuestion").value(nextQuestion))
                .andExpect(jsonPath("$.data.nextTurn.translatedQuestion").value(translatedQuestion))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(nextSequence))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(false));
    }

    private long startSession(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/sessions", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sessionId").isNumber())
                .andExpect(jsonPath("$.data.scenarioId").value(scenarioId))
                .andExpect(jsonPath("$.data.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.currentTurn.turnId").isNumber())
                .andExpect(jsonPath("$.data.currentTurn.sequence").value(1))
                .andExpect(jsonPath("$.data.currentTurn.aiQuestion").value("What is your favorite food? Why do you like it?"))
                .andExpect(jsonPath("$.data.currentTurn.translatedQuestion").value("가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?"))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(1))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(false))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return body.get("data").get("sessionId").asLong();
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
    static class ScenarioFlowTestConfiguration {

        @Bean
        @Primary
        TestAiConversationClient testAiConversationClient() {
            return new TestAiConversationClient();
        }
    }

    static class TestAiConversationClient implements AiConversationClient {

        private final List<AiNextQuestionRequest> nextQuestionRequests = new ArrayList<>();
        private final List<AiTurnFeedbackRequest> turnFeedbackRequests = new ArrayList<>();
        private AiSessionFeedbackRequest sessionFeedbackRequest;

        void reset() {
            nextQuestionRequests.clear();
            turnFeedbackRequests.clear();
            sessionFeedbackRequest = null;
        }

        @Override
        public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
            nextQuestionRequests.add(request);
            return switch (request.nextQuestion().sequence()) {
                case 2 -> new AiNextQuestionResponse(
                        "That sounds tasty. Do you cook often?",
                        "맛있겠네요. 요리는 자주 하나요?");
                case 3 -> new AiNextQuestionResponse(
                        "Nice, weekends are a good time to cook. What food did you eat recently?",
                        "좋아요, 주말은 요리하기 좋죠. 최근에 먹은 음식은 뭐였나요?");
                case 4 -> new AiNextQuestionResponse(
                        "Ramen is a familiar comfort food. Is there a food you want to try next?",
                        "라면은 익숙한 편안한 음식이죠. 다음에 먹어보고 싶은 음식이 있나요?");
                default -> throw new IllegalArgumentException("unexpected next sequence");
            };
        }

        @Override
        public AiTurnFeedbackStatusResponse generateTurnFeedback(AiTurnFeedbackRequest request) {
            turnFeedbackRequests.add(request);
            return new AiTurnFeedbackStatusResponse(request.sessionId(), request.turnId(), TurnFeedbackStatus.PREPARING);
        }

        @Override
        public AiSessionFeedbackResponse generateSessionFeedback(AiSessionFeedbackRequest request) {
            sessionFeedbackRequest = request;
            return new AiSessionFeedbackResponse(
                    request.sessionId(),
                    82,
                    "유학생 수준",
                    "하고 싶은 말을 끝까지 전달했고 이유를 붙이는 힘이 좋았어요.",
                    List.of(
                            good(request.expectedTurnIds().get(0)),
                            improvement(request.expectedTurnIds().get(1)),
                            good(request.expectedTurnIds().get(2)),
                            good(request.expectedTurnIds().get(3))));
        }

        private AiSessionTurnFeedbackResponse good(Long turnId) {
            return new AiSessionTurnFeedbackResponse(
                    turnId,
                    FeedbackType.GOOD,
                    "한국어로 비유하자면 담백하게 이유를 붙인 말처럼 들려요.",
                    null,
                    null,
                    null,
                    "이유를 자연스럽게 붙였어요.",
                    "좋아하는 것과 이유를 한 문장 안에서 분명하게 연결했기 때문이에요.");
        }

        private AiSessionTurnFeedbackResponse improvement(Long turnId) {
            return new AiSessionTurnFeedbackResponse(
                    turnId,
                    FeedbackType.NEEDS_IMPROVEMENT,
                    "한국어로 비유하자면 조금 단어만 놓고 말한 느낌이에요.",
                    "on weekends 앞에 자연스러운 빈도 표현을 붙이면 좋아요.",
                    "습관을 말할 때는 usually 같은 부사를 쓰면 더 자연스럽게 들려요.",
                    "I usually cook pasta on weekends.",
                    null,
                    null);
        }
    }
}
