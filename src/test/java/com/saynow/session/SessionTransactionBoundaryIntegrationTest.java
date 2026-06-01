// 세션 발화 처리에서 원격 AI 호출이 DB 트랜잭션 밖에서 실행되는지 검증하는 통합 테스트
package com.saynow.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.session.infrastructure.ai.AiConversationClient;
import com.saynow.session.infrastructure.ai.AiFeedbackRequest;
import com.saynow.session.infrastructure.ai.AiFeedbackResponse;
import com.saynow.session.infrastructure.ai.AiFilledSlot;
import com.saynow.session.infrastructure.ai.AiGuideRequest;
import com.saynow.session.infrastructure.ai.AiGuideResponse;
import com.saynow.session.infrastructure.ai.AiNextQuestionRequest;
import com.saynow.session.infrastructure.ai.AiNextQuestionResponse;
import com.saynow.session.infrastructure.ai.AiNextQuestionSlotStatus;
import com.saynow.session.infrastructure.ai.AiTurnFeedbackResponse;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionTransactionBoundaryIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TransactionBoundaryAiConversationClient aiConversationClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiConversationClient.reset();
    }

    @Test
    void nextQuestionAiCallRunsOutsideDatabaseTransaction() throws Exception {
        String accessToken = login("tx-boundary-sub|tx-boundary@example.com|Tx Boundary User");
        long sessionId = startSession(accessToken, 4);

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isOk());

        assertThat(aiConversationClient.nextQuestionTransactionActiveValues())
                .containsExactly(false);
    }

    @Test
    void nextQuestionFailureDoesNotRecordUserUtterance() throws Exception {
        String accessToken = login("tx-failure-sub|tx-failure@example.com|Tx Failure User");
        long sessionId = startSession(accessToken, 4);
        aiConversationClient.failNextQuestion();

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isInternalServerError());

        Integer answeredTurnCount = jdbcTemplate.queryForObject(
                "select count(*) from session_turns where session_id = ? and user_utterance is not null",
                Integer.class,
                sessionId);
        assertThat(answeredTurnCount).isZero();
    }

    private long startSession(String accessToken, long scenarioId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/scenarios/{scenarioId}/sessions", scenarioId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isCreated())
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
    static class TransactionBoundaryTestConfiguration {

        @Bean
        @Primary
        TransactionBoundaryAiConversationClient transactionBoundaryAiConversationClient() {
            return new TransactionBoundaryAiConversationClient();
        }
    }

    static class TransactionBoundaryAiConversationClient implements AiConversationClient {

        private final List<Boolean> nextQuestionTransactionActiveValues = new ArrayList<>();
        private boolean failNextQuestion;

        void reset() {
            nextQuestionTransactionActiveValues.clear();
            failNextQuestion = false;
        }

        void failNextQuestion() {
            failNextQuestion = true;
        }

        List<Boolean> nextQuestionTransactionActiveValues() {
            return nextQuestionTransactionActiveValues;
        }

        @Override
        public AiNextQuestionResponse generateNextQuestion(AiNextQuestionRequest request) {
            nextQuestionTransactionActiveValues.add(TransactionSynchronizationManager.isActualTransactionActive());
            if (failNextQuestion) {
                throw new ApiException(ErrorCode.AI_GENERATION_FAILED);
            }

            List<AiNextQuestionSlotStatus> slots = request.slots() == null ? List.of() : request.slots();
            List<AiFilledSlot> newlyFilled = slots.stream()
                    .filter(slot -> !slot.filled())
                    .map(slot -> new AiFilledSlot(slot.slotName()))
                    .findFirst()
                    .stream()
                    .toList();

            String nextSlot = slots.stream()
                    .filter(slot -> !slot.filled())
                    .map(AiNextQuestionSlotStatus::slotName)
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
            List<AiTurnFeedbackResponse> turnFeedbacks = request.turns().stream()
                    .map(turn -> new AiTurnFeedbackResponse(
                            turn.turnId(),
                            true,
                            "의도는 전달됐지만 표현이 조금 짧게 들립니다.",
                            "한국어로 치면 필요한 것만 짧게 말한 느낌입니다.",
                            "I'd like " + turn.userUtterance() + ", please."))
                    .toList();
            return new AiFeedbackResponse(
                    82,
                    "전체적으로 의도는 전달됐고, 조금 더 자연스러운 표현을 연습하면 좋습니다.",
                    turnFeedbacks);
        }

        @Override
        public AiGuideResponse generateGuide(AiGuideRequest request) {
            return new AiGuideResponse("영어 표현 질문으로 이해했어요.");
        }
    }
}
