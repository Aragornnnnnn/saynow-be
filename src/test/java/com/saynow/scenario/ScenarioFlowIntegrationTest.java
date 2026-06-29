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
    void roommateScenarioCompletesAfterThreeAnswersAndFinalFeedbackUnlocksNextScenario() throws Exception {
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
                        .value("Hey, you're my roommate, right?! I'm Charlie, nice to meet you! What's your name? Tell me a little about yourself!"))
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].firstQuestionPreview.translatedQuestion")
                        .value("안녕 너 내 룸메이트지?! 난 charlie야. 만나서 반가워. 넌 이름이 뭐야? 너에 대해 소개해주라."))
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
                "Hi, I'm Joe. I'm studying business, and I like playing games after class.",
                1,
                2,
                "Nice to meet you. What are you into? What do you love about it?",
                "만나서 반가워. 취미는 뭐야? 그게 어떤 매력이 있어?",
                "이름과 자기소개를 자연스럽게 말해줘서 첫 대화가 편하게 시작됐다.",
                "GOOD");
        submitAndExpectNext(
                accessToken,
                sessionId,
                "I'm into hiking because it helps me clear my head and see new places.",
                2,
                3,
                "That's cool. I'm obsessed with Korea! Tell me your must-visit spots and why I should go!",
                "좋다. 나 한국 엄청 좋아하는데, 추천할 만한 관광지와 그 이유를 알려줘!",
                "취미를 좋아하는 이유까지 말해줘서 어떤 사람인지 더 잘 보인다.",
                "GOOD");
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"You should visit Gyeongju because it has beautiful old temples and a calm atmosphere."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.submittedTurn.sequence").value(3))
                .andExpect(jsonPath("$.data.submittedTurn.turnFeedbackStatus").value("PREPARING"))
                .andExpect(jsonPath("$.data.submittedTurn.innerThought").value("추천 장소와 이유를 같이 말해줘서 바로 가보고 싶어진다."))
                .andExpect(jsonPath("$.data.submittedTurn.innerThoughtType").value("GOOD"))
                .andExpect(jsonPath("$.data.nextTurn.sequence").value(4))
                .andExpect(jsonPath("$.data.nextTurn.aiQuestion").value("That sounds amazing. I’ll definitely add it to my Korea list."))
                .andExpect(jsonPath("$.data.nextTurn.translatedQuestion").value("정말 좋다. 한국에서 가볼 곳 리스트에 꼭 넣어둘게."))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(4))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(3))
                .andExpect(jsonPath("$.data.progress.completed").value(true));

        assertThat(aiConversationClient.nextQuestionRequests).hasSize(2);
        assertThat(aiConversationClient.closingMessageRequests).hasSize(1);
        assertThat(aiConversationClient.turnFeedbackRequests).hasSize(3);
        assertThat(aiConversationClient.nextQuestionTransactionActive).containsOnly(false);
        assertThat(aiConversationClient.closingMessageTransactionActive).containsOnly(false);
        assertThat(aiConversationClient.turnFeedbackTransactionActive).containsOnly(false);
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().currentTurn().userUtterance())
                .isEqualTo("Hi, I'm Joe. I'm studying business, and I like playing games after class.");
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().nextQuestion().sequence())
                .isEqualTo(2);
        assertThat(aiConversationClient.nextQuestionRequests.getFirst().scenario().counterpartRole())
                .isEqualTo("roommate");
        assertThat(aiConversationClient.closingMessageRequests.getFirst().submittedSequence()).isEqualTo(3);
        assertThat(aiConversationClient.closingMessageRequests.getFirst().closingReason()).isEqualTo(ClosingReason.MAX_TURNS_REACHED);
        assertThat(aiConversationClient.closingMessageRequests.getFirst().goalCompletionStatus()).isEqualTo(GoalCompletionStatus.COMPLETED);
        assertThat(aiConversationClient.closingMessageRequests.getFirst().currentTurn().userUtterance())
                .isEqualTo("You should visit Gyeongju because it has beautiful old temples and a calm atmosphere.");

        List<String> storedTurns = jdbcTemplate.queryForList("""
                SELECT sequence || '|' || ai_question || '|' || translated_question || '|' || COALESCE(user_utterance, '<NULL>') || '|' || COALESCE(inner_thought, '<NULL>') || '|' || COALESCE(inner_thought_type, '<NULL>')
                FROM session_turns
                WHERE session_id = ?
                ORDER BY sequence
                """, String.class, sessionId);
        assertThat(storedTurns).hasSize(4);
        assertThat(storedTurns.get(2)).contains("3|", "You should visit Gyeongju", "추천 장소와 이유를 같이 말해줘서 바로 가보고 싶어진다.|GOOD");
        assertThat(storedTurns.get(3)).isEqualTo("4|That sounds amazing. I’ll definitely add it to my Korea list.|정말 좋다. 한국에서 가볼 곳 리스트에 꼭 넣어둘게.|<NULL>|<NULL>|<NULL>");

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
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(3))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].sequence").value(1))
                .andExpect(jsonPath("$.data.turnFeedbacks[0].originalQuestion")
                        .value("Hey, you're my roommate, right?! I'm Charlie, nice to meet you! What's your name? Tell me a little about yourself!"))
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

        assertThat(aiConversationClient.sessionFeedbackRequest.expectedTurnIds()).hasSize(3);
        assertThat(aiConversationClient.sessionFeedbackRequest.expectedTurnIds())
                .doesNotContain(jdbcTemplate.queryForObject("""
                        SELECT id
                        FROM session_turns
                        WHERE session_id = ?
                          AND sequence = 4
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
        assertThat(turnFeedbackCount).isEqualTo(3);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.nativeScore").value(82))
                .andExpect(jsonPath("$.data.highlightMessage").value("한국인의 40%가 헷갈리는 간접의문문 어순을 피해 간 사람이에요."))
                .andExpect(jsonPath("$.data.turnFeedbacks.length()").value(3));
        assertThat(aiConversationClient.sessionFeedbackRequestCount).isEqualTo(1);

        mockMvc.perform(get("/api/v1/scenarios")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.categories[0].scenarios[0].completed").value(true))
                .andExpect(jsonPath("$.data.categories[0].scenarios[1].locked").value(false));
    }

    @Test
    void feedbackRequiresAllThreeAnswers() throws Exception {
        String accessToken = login("mvp3-sub-2|feedback-not-ready@example.com|Not Ready User");
        long sessionId = startSession(accessToken, 1);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/feedback", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SESSION_NOT_COMPLETED"));
    }

    @Test
    void submitCompletesAtTotalQuestionCountEvenIfLegacyQuestionRowExists() throws Exception {
        String accessToken = login("mvp3-sub-legacy|legacy@example.com|Legacy User");
        long sessionId = startSession(accessToken, 1);

        submitAndExpectNext(
                accessToken,
                sessionId,
                "Hi, I'm Mia. I'm studying design.",
                1,
                2,
                "Nice to meet you. What are you into? What do you love about it?",
                "만나서 반가워. 취미는 뭐야? 그게 어떤 매력이 있어?",
                "이름과 자기소개를 자연스럽게 말해줘서 첫 대화가 편하게 시작됐다.",
                "GOOD");
        submitAndExpectNext(
                accessToken,
                sessionId,
                "I'm into photography because it helps me notice small details.",
                2,
                3,
                "That's cool. I'm obsessed with Korea! Tell me your must-visit spots and why I should go!",
                "좋다. 나 한국 엄청 좋아하는데, 추천할 만한 관광지와 그 이유를 알려줘!",
                "취미를 좋아하는 이유까지 말해줘서 어떤 사람인지 더 잘 보인다.",
                "GOOD");

        jdbcTemplate.update("""
                INSERT INTO scenario_questions (scenario_id, sequence, question_en, question_ko, created_at, updated_at)
                VALUES (1, 4, 'Legacy question beyond total count', '총 질문 수 밖의 과거 질문', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        try {
            mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"userUtterance":"You should visit Jeju because the nature is beautiful."}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.submittedTurn.sequence").value(3))
                    .andExpect(jsonPath("$.data.nextTurn.sequence").value(4))
                    .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(3))
                    .andExpect(jsonPath("$.data.progress.completed").value(true));

            assertThat(aiConversationClient.nextQuestionRequests).hasSize(2);
            assertThat(aiConversationClient.closingMessageRequests).hasSize(1);
        } finally {
            jdbcTemplate.update("""
                    DELETE FROM scenario_questions
                    WHERE scenario_id = 1
                      AND sequence = 4
                      AND question_en = 'Legacy question beyond total count'
                    """);
        }
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
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(3))
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
                .andExpect(jsonPath("$.data.totalQuestionCount").value(3))
                .andExpect(jsonPath("$.data.currentTurn.turnId").isNumber())
                .andExpect(jsonPath("$.data.currentTurn.sequence").value(1))
                .andExpect(jsonPath("$.data.currentTurn.aiQuestion").value("Hey, you're my roommate, right?! I'm Charlie, nice to meet you! What's your name? Tell me a little about yourself!"))
                .andExpect(jsonPath("$.data.currentTurn.translatedQuestion").value("안녕 너 내 룸메이트지?! 난 charlie야. 만나서 반가워. 넌 이름이 뭐야? 너에 대해 소개해주라."))
                .andExpect(jsonPath("$.data.progress.currentSequence").value(1))
                .andExpect(jsonPath("$.data.progress.totalQuestionCount").value(3))
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
                        "Nice to meet you. What are you into? What do you love about it?",
                        "만나서 반가워. 취미는 뭐야? 그게 어떤 매력이 있어?",
                        "이름과 자기소개를 자연스럽게 말해줘서 첫 대화가 편하게 시작됐다.",
                        InnerThoughtType.GOOD);
                case 3 -> new AiNextQuestionResponse(
                        "That's cool. I'm obsessed with Korea! Tell me your must-visit spots and why I should go!",
                        "좋다. 나 한국 엄청 좋아하는데, 추천할 만한 관광지와 그 이유를 알려줘!",
                        "취미를 좋아하는 이유까지 말해줘서 어떤 사람인지 더 잘 보인다.",
                        InnerThoughtType.GOOD);
                default -> throw new IllegalArgumentException("unexpected next sequence");
            };
        }

        @Override
        public AiClosingMessageResponse generateClosingMessage(AiClosingMessageRequest request) {
            closingMessageRequests.add(request);
            closingMessageTransactionActive.add(TransactionSynchronizationManager.isActualTransactionActive());
            return new AiClosingMessageResponse(
                    "That sounds amazing. I’ll definitely add it to my Korea list.",
                    "정말 좋다. 한국에서 가볼 곳 리스트에 꼭 넣어둘게.",
                    "추천 장소와 이유를 같이 말해줘서 바로 가보고 싶어진다.",
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
                            good(request.expectedTurnIds().get(2))));
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
