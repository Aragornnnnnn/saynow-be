// 3차 MVP 프리톡 시나리오 목록, 세션 진행, 최종 피드백 흐름을 통합 검증한다.
package com.saynow.scenario;

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
import com.saynow.session.infrastructure.ai.ClosingReason;
import com.saynow.session.domain.InnerThoughtType;
import com.saynow.session.infrastructure.ai.FeedbackType;
import com.saynow.session.infrastructure.ai.GoalCompletionStatus;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    void roommateScenarioCompletesAfterFourAnswersAndFinalFeedbackUnlocksNextScenario() throws Exception {
        String accessToken = login("mvp3-sub-1|mvp3@example.com|MVP3 User");

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].categoryName").value("룸메이트"))
                .andExpect(jsonPath("$.data.categories[0].categoryLocked").value(false))
                .andExpect(jsonPath("$.data.categories[0].categoryLockReason").value(nullValue()))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].scenarioTitle").value("입주 첫날 — charlie와 첫 만남"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].briefing").isString())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].conversationGoal").isString())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].locked").value(false))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.questionId").isNumber())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.aiQuestion")
                        .value("Hey, you must be my roommate! I'm charlie. Okay, tell me everything — what are you studying, what are you into?"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.translatedQuestion")
                        .value("야 너 내 룸메지! 난 charlie야. 자, 다 얘기해봐 — 뭐 전공하고 뭐 좋아해?"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].lockReason").value("PREVIOUS_SCENARIO_NOT_COMPLETED"))
                .andExpect(jsonPath("$.data.categories[1].categoryName").value("수업"))
                .andExpect(jsonPath("$.data.categories[1].categoryLocked").value(true))
                .andExpect(jsonPath("$.data.categories[1].categoryLockReason").value("COMING_SOON"));

        long sessionId = startSession(accessToken, 1);
        String openingInnerThoughtType = jdbcTemplate.queryForObject("""
                SELECT inner_thought_type
                FROM session_turns
                WHERE session_id = ?
                  AND sequence = 1
                """, String.class, sessionId);
        assertThat(openingInnerThoughtType).isEqualTo("NORMAL");

        submitAndExpectNext(
                accessToken,
                sessionId,
                "I study business, and I like playing games after class.",
                1,
                2,
                "Nice. What made you decide to come all the way here?",
                "좋아. 너는 어쩌다 여기까지 오게 된 거야?",
                "첫 만남인데 전공과 취미를 자연스럽게 알려줘서 말 걸기 편하겠다.",
                "GOOD");
        submitAndExpectNext(
                accessToken,
                sessionId,
                "I came here because I wanted to improve my English and live more independently.",
                2,
                3,
                "That makes sense. How should we split the cleaning and stuff?",
                "그럴 만하네. 청소 같은 건 어떻게 나눌까?",
                "혼자 살아보려는 이유가 분명해서 앞으로 룸메이트 생활도 잘 맞춰갈 수 있겠다.",
                "GOOD");
        submitAndExpectNext(
                accessToken,
                sessionId,
                "I think a simple schedule would be helpful, but we can change it if needed.",
                3,
                4,
                "Sounds fair. I'm making dinner tonight — is there anything you really can't eat?",
                "좋아. 오늘 저녁 내가 하는데, 진짜 못 먹는 거 있어?",
                "스케줄을 선호한다고 말하면서도 조정 가능하다고 해서 같이 살기 편하겠다.",
                "GOOD");
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"Thanks, I'd love to share. I can't really eat fish, but anything else is fine."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedTurn.sequence").value(4))
                .andExpect(jsonPath("$.data.submittedTurn.turnFeedbackStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.submittedTurn.innerThought").value("못 먹는 음식을 부드럽게 말해줘서 저녁 메뉴를 맞추기 쉽겠다."))
                .andExpect(jsonPath("$.data.submittedTurn.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.data.nextTurn.sequence").value(5))
                .andExpect(jsonPath("$.data.nextTurn.aiQuestion").value("Got it. I'll avoid fish, and we can share dinner tonight."))
                .andExpect(jsonPath("$.data.nextTurn.translatedQuestion").value("알겠어. 생선은 피해서 오늘 저녁 같이 먹자."))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(5))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(true));

        assertThat(aiConversationClient.nextQuestionRequests).hasSize(3);
        assertThat(aiConversationClient.closingMessageRequests).hasSize(1);
        assertThat(aiConversationClient.turnFeedbackRequests).hasSize(4);
        assertThat(aiConversationClient.nextQuestionTransactionActive).containsOnly(false);
        assertThat(aiConversationClient.closingMessageTransactionActive).containsOnly(false);
        assertThat(aiConversationClient.turnFeedbackTransactionActive).containsOnly(false);
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().currentTurn().userUtterance())
                .isEqualTo("I study business, and I like playing games after class.");
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().nextQuestion().sequence())
                .isEqualTo(2);
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().scenario().counterpartRole())
                .isEqualTo("roommate");
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().scenario().serviceAudience().name())
                .isEqualTo("KOREAN_LEARNER");
        assertThat(aiConversationClient.closingMessageRequests.getFirst().submittedSequence()).isEqualTo(4);
        assertThat(aiConversationClient.closingMessageRequests.getFirst().closingReason()).isEqualTo(ClosingReason.MAX_TURNS_REACHED);
        assertThat(aiConversationClient.closingMessageRequests.getFirst().goalCompletionStatus()).isEqualTo(GoalCompletionStatus.COMPLETED);
        assertThat(aiConversationClient.closingMessageRequests.getFirst().scenario().serviceAudience().name())
                .isEqualTo("KOREAN_LEARNER");
        assertThat(aiConversationClient.closingMessageRequests.getFirst().currentTurn().userUtterance())
                .isEqualTo("Thanks, I'd love to share. I can't really eat fish, but anything else is fine.");
        assertThat(aiConversationClient.turnFeedbackRequests.getFirst().scenario().serviceAudience().name())
                .isEqualTo("KOREAN_LEARNER");

        List<String> storedTurns = jdbcTemplate.queryForList("""
                SELECT sequence || '|' || ai_question || '|' || translated_question || '|' || COALESCE(user_utterance, '<NULL>') || '|' || COALESCE(inner_thought, '<NULL>') || '|' || COALESCE(inner_thought_type, '<NULL>')
                FROM session_turns
                WHERE session_id = ?
                ORDER BY sequence
                """, String.class, sessionId);
        assertThat(storedTurns).hasSize(5);
        assertThat(storedTurns.get(3)).contains("4|", "Thanks, I'd love to share", "못 먹는 음식을 부드럽게 말해줘서 저녁 메뉴를 맞추기 쉽겠다.|GOOD");
        assertThat(storedTurns.get(4)).isEqualTo("5|Got it. I'll avoid fish, and we can share dinner tonight.|알겠어. 생선은 피해서 오늘 저녁 같이 먹자.|<NULL>|<NULL>|<NULL>");

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
                .andExpect(jsonPath("$.data.nativeScoreBreakdown").doesNotExist())
                .andExpect(jsonPath("$.data.highlightMessage").value("한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요."))
                .andExpect(jsonPath("$.data.nativeLevelLabel").doesNotExist())
                .andExpect(jsonPath("$.data.summary").doesNotExist())
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(4))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].sequence").value(1))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].originalQuestion")
                        .value("Hey, you must be my roommate! I'm charlie. Okay, tell me everything — what are you studying, what are you into?"))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].feedbackType").value("GOOD"))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].koreanAnalogy").isString())
                .andExpect(jsonPath("$.data.turnFeedbacks[0].positiveFeedback").value(nullValue()))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].feedbackDetail").isString())
                .andExpect(jsonPath("$.data.turnFeedbacks[0].correctionExpression").value(nullValue()))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].correctionReason").value(nullValue()))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].benchmarkMessage").isString())
                .andExpect(jsonPath("$.data.turnFeedbacks[0].betterExpression").doesNotExist())
                .andExpect(jsonPath("$.data.turnFeedbacks[1].feedbackType").value("NEEDS_IMPROVEMENT"))
                .andExpect(jsonPath("$.data.turnFeedbacks[1].positiveFeedback").isString())
                .andExpect(jsonPath("$.data.turnFeedbacks[1].feedbackDetail").value(nullValue()))
                .andExpect(jsonPath("$.data.turnFeedbacks[1].correctionExpression").value("I prefer traveling alone."))
                .andExpect(jsonPath("$.data.turnFeedbacks[1].correctionReason").value("prefer 뒤에는 동명사나 명사를 쓰면 더 자연스러워요."))
                .andExpect(jsonPath("$.data.turnFeedbacks[1].benchmarkMessage").value(nullValue()))
                .andExpect(jsonPath("$.data.turnFeedbacks[1].correctionPoint").doesNotExist())
                .andExpect(jsonPath("$.data.turnFeedbacks[1].plusOneExpression").doesNotExist())
                .andExpect(jsonPath("$.data.turnFeedbacks[1].praiseReason").doesNotExist());

        assertThat(aiConversationClient.sessionFeedbackRequest.expectedTurnIds()).hasSize(4);
        assertThat(aiConversationClient.sessionFeedbackRequest.scenario().serviceAudience().name())
                .isEqualTo("KOREAN_LEARNER");
        assertThat(aiConversationClient.sessionFeedbackRequest.expectedTurnIds())
                .doesNotContain(jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM session_turns
                        WHERE session_id = ?
                          AND sequence = 5
                        """, Long.class, sessionId));
        assertThat(aiConversationClient.sessionFeedbackTransactionActive).isFalse();
        assertThat(aiConversationClient.sessionFeedbackRequestCount).isEqualTo(1);
        Integer sessionFeedbackCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM session_feedbacks
                WHERE session_id = ?
                  AND native_score = 82
                  AND highlight_message = '한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요.'
                """, Integer.class, sessionId);
        assertThat(sessionFeedbackCount).isEqualTo(1);

        Integer turnFeedbackCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM turn_feedbacks tf
                JOIN session_turns st ON st.id = tf.turn_id
                WHERE st.session_id = ?
                """, Integer.class, sessionId);
        assertThat(turnFeedbackCount).isEqualTo(4);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.nativeScore").value(82))
                .andExpect(jsonPath("$.data.highlightMessage").value("한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요."))
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(4));
        assertThat(aiConversationClient.sessionFeedbackRequestCount).isEqualTo(1);

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
            String translatedQuestion,
            String innerThought,
            String innerThoughtType
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
                .andExpect(jsonPath("$.data.submittedTurn.innerThought").value(innerThought))
                .andExpect(jsonPath("$.data.submittedTurn.innerThoughtType").value(innerThoughtType))
                .andExpect(jsonPath("$.data.nextTurn.sequence").value(nextSequence))
                .andExpect(jsonPath("$.data.nextTurn.aiQuestion").value(nextQuestion))
                .andExpect(jsonPath("$.data.nextTurn.translatedQuestion").value(translatedQuestion))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(nextSequence))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(4))
                .andExpect(jsonPath("$.data.progress.completed").value(false));
        String storedInnerThought = jdbcTemplate.queryForObject("""
                SELECT inner_thought
                FROM session_turns
                WHERE session_id = ?
                  AND sequence = ?
                """, String.class, sessionId, submittedSequence);
        assertThat(storedInnerThought).isEqualTo(innerThought);
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
                .andExpect(jsonPath("$.data.currentTurn.aiQuestion").value("Hey, you must be my roommate! I'm charlie. Okay, tell me everything — what are you studying, what are you into?"))
                .andExpect(jsonPath("$.data.currentTurn.translatedQuestion").value("야 너 내 룸메지! 난 charlie야. 자, 다 얘기해봐 — 뭐 전공하고 뭐 좋아해?"))
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
        private final List<AiClosingMessageRequest> closingMessageRequests = new ArrayList<>();
        private final List<AiTurnFeedbackRequest> turnFeedbackRequests = new ArrayList<>();
        private final List<Boolean> nextQuestionTransactionActive = new ArrayList<>();
        private final List<Boolean> closingMessageTransactionActive = new ArrayList<>();
        private final List<Boolean> turnFeedbackTransactionActive = new ArrayList<>();
        private AiSessionFeedbackRequest sessionFeedbackRequest;
        private int sessionFeedbackRequestCount;
        private boolean sessionFeedbackTransactionActive;

        void reset() {
            nextQuestionRequests.clear();
            closingMessageRequests.clear();
            turnFeedbackRequests.clear();
            nextQuestionTransactionActive.clear();
            closingMessageTransactionActive.clear();
            turnFeedbackTransactionActive.clear();
            sessionFeedbackRequest = null;
            sessionFeedbackRequestCount = 0;
            sessionFeedbackTransactionActive = false;
        }

        @Override
        public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
            nextQuestionRequests.add(request);
            nextQuestionTransactionActive.add(TransactionSynchronizationManager.isActualTransactionActive());
            return switch (request.nextQuestion().sequence()) {
                case 2 -> new AiNextQuestionResponse(
                        "Nice. What made you decide to come all the way here?",
                        "좋아. 너는 어쩌다 여기까지 오게 된 거야?",
                        "첫 만남인데 전공과 취미를 자연스럽게 알려줘서 말 걸기 편하겠다.",
                        InnerThoughtType.GOOD);
                case 3 -> new AiNextQuestionResponse(
                        "That makes sense. How should we split the cleaning and stuff?",
                        "그럴 만하네. 청소 같은 건 어떻게 나눌까?",
                        "혼자 살아보려는 이유가 분명해서 앞으로 룸메이트 생활도 잘 맞춰갈 수 있겠다.",
                        InnerThoughtType.GOOD);
                case 4 -> new AiNextQuestionResponse(
                        "Sounds fair. I'm making dinner tonight — is there anything you really can't eat?",
                        "좋아. 오늘 저녁 내가 하는데, 진짜 못 먹는 거 있어?",
                        "스케줄을 선호한다고 말하면서도 조정 가능하다고 해서 같이 살기 편하겠다.",
                        InnerThoughtType.GOOD);
                default -> throw new IllegalArgumentException("unexpected next sequence");
            };
        }

        @Override
        public AiClosingMessageResponse generateClosingMessage(AiClosingMessageRequest request) {
            closingMessageRequests.add(request);
            closingMessageTransactionActive.add(TransactionSynchronizationManager.isActualTransactionActive());
            return new AiClosingMessageResponse(
                    "Got it. I'll avoid fish, and we can share dinner tonight.",
                    "알겠어. 생선은 피해서 오늘 저녁 같이 먹자.",
                    "못 먹는 음식을 부드럽게 말해줘서 저녁 메뉴를 맞추기 쉽겠다.",
                    InnerThoughtType.GOOD);
        }

        @Override
        public AiTurnFeedbackStatusResponse generateTurnFeedback(AiTurnFeedbackRequest request) {
            turnFeedbackRequests.add(request);
            turnFeedbackTransactionActive.add(TransactionSynchronizationManager.isActualTransactionActive());
            return new AiTurnFeedbackStatusResponse(request.sessionId(), request.turnId(), TurnFeedbackStatus.PREPARING);
        }

        @Override
        public AiSessionFeedbackResponse generateSessionFeedback(AiSessionFeedbackRequest request) {
            sessionFeedbackRequest = request;
            sessionFeedbackRequestCount++;
            sessionFeedbackTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
            return new AiSessionFeedbackResponse(
                    request.sessionId(),
                    82,
                    "한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요.",
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
                    "좋아하는 것과 이유를 한 문장 안에서 분명하게 연결했기 때문이에요.",
                    null,
                    null,
                    "한국인의 35%가 틀리는 표현인데 정확히 맞췄어요.");
        }

        private AiSessionTurnFeedbackResponse improvement(Long turnId) {
            return new AiSessionTurnFeedbackResponse(
                    turnId,
                    FeedbackType.NEEDS_IMPROVEMENT,
                    "한국어로 비유하자면 조금 단어만 놓고 말한 느낌이에요.",
                    "어려운 표현에 도전한 점은 좋아요. 틀리는 것보다 시도한 게 더 중요해요.",
                    null,
                    "I prefer traveling alone.",
                    "prefer 뒤에는 동명사나 명사를 쓰면 더 자연스러워요.",
                    null);
        }
    }
}
