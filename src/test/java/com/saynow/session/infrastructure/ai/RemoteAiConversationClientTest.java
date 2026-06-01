// 원격 AI 서버의 3차 MVP 대화와 피드백 계약을 검증한다.
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteAiConversationClientTest {

    private HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        RequestTraceContext.clear();
    }

    @Test
    void nextQuestionRequestUsesFixedNextQuestionContractAndMapsResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        RemoteAiConversationClient client = clientWithResponse(
                "/api/v1/conversation/next-question",
                requestBody,
                requestIdHeader,
                """
                        {
                          "aiQuestion":"That sounds tasty. Do you cook often?",
                          "translatedQuestion":"맛있겠네요. 요리는 자주 하나요?"
                        }
                        """);
        RequestTraceContext.start("trace-next-question-3rd");

        AiNextQuestionResponse response = client.generateNextQuestion(nextQuestionRequest());

        JsonNode json = objectMapper.readTree(requestBody.get());
        assertThat(requestIdHeader.get()).isEqualTo("trace-next-question-3rd");
        assertThat(json.get("sessionId").asLong()).isEqualTo(1000L);
        assertThat(json.get("submittedTurnId").asLong()).isEqualTo(5000L);
        assertThat(json.get("submittedSequence").asInt()).isEqualTo(1);
        assertThat(json.get("scenario").get("scenarioId").asLong()).isEqualTo(10L);
        assertThat(json.get("scenario").get("title").asText()).isEqualTo("음식 취향 이야기하기");
        assertThat(json.get("scenario").get("briefing").asText()).contains("좋아하는 음식");
        assertThat(json.get("scenario").get("conversationGoal").asText()).contains("음식 취향");
        assertThat(json.get("currentTurn").get("aiQuestion").asText())
                .isEqualTo("What is your favorite food? Why do you like it?");
        assertThat(json.get("currentTurn").get("translatedQuestion").asText())
                .isEqualTo("가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?");
        assertThat(json.get("currentTurn").get("userUtterance").asText())
                .isEqualTo("I like pizza because it is spicy.");
        assertThat(json.get("nextQuestion").get("questionId").asLong()).isEqualTo(101L);
        assertThat(json.get("nextQuestion").get("sequence").asInt()).isEqualTo(2);
        assertThat(json.get("nextQuestion").get("questionEn").asText()).isEqualTo("Do you cook often?");
        assertThat(response.aiQuestion()).isEqualTo("That sounds tasty. Do you cook often?");
        assertThat(response.translatedQuestion()).isEqualTo("맛있겠네요. 요리는 자주 하나요?");
    }

    @Test
    void turnFeedbackRequestUsesCacheContractAndMapsPreparingStatus() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        RemoteAiConversationClient client = clientWithResponse(
                "/api/v1/conversation/turn-feedback",
                requestBody,
                new AtomicReference<>(),
                """
                        {
                          "sessionId":1000,
                          "turnId":5000,
                          "feedbackStatus":"PREPARING"
                        }
                        """);

        AiTurnFeedbackStatusResponse response = client.generateTurnFeedback(turnFeedbackRequest());

        JsonNode json = objectMapper.readTree(requestBody.get());
        assertThat(json.get("sessionId").asLong()).isEqualTo(1000L);
        assertThat(json.get("turnId").asLong()).isEqualTo(5000L);
        assertThat(json.get("sequence").asInt()).isEqualTo(1);
        assertThat(json.get("turn").get("userUtterance").asText())
                .isEqualTo("I like pizza because it is spicy.");
        assertThat(response.feedbackStatus()).isEqualTo(TurnFeedbackStatus.PREPARING);
    }

    @Test
    void sessionFeedbackRequestUsesExpectedTurnIdsAndMapsQualityFeedback() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        RemoteAiConversationClient client = clientWithResponse(
                "/api/v1/conversation/session-feedback",
                requestBody,
                new AtomicReference<>(),
                """
                        {
                          "sessionId":1000,
                          "nativeScore":82,
                          "nativeLevelLabel":"유학생 수준",
                          "summary":"하고 싶은 말을 끝까지 전달하는 힘이 좋았어요.",
                          "turnFeedbacks":[
                            {
                              "turnId":5000,
                              "feedbackType":"GOOD",
                              "koreanAnalogy":"한국어로 비유하자면 담백하게 들려요.",
                              "correctionPoint":null,
                              "correctionReason":null,
                              "plusOneExpression":null,
                              "praiseSummary":"이유를 자연스럽게 붙였어요.",
                              "praiseReason":"좋아하는 음식과 이유를 분명하게 연결했어요."
                            }
                          ]
                        }
                        """);

        AiSessionFeedbackResponse response = client.generateSessionFeedback(sessionFeedbackRequest());

        JsonNode json = objectMapper.readTree(requestBody.get());
        assertThat(json.get("sessionId").asLong()).isEqualTo(1000L);
        assertThat(json.get("scenario").get("scenarioId").asLong()).isEqualTo(10L);
        assertThat(json.get("expectedTurnIds").get(0).asLong()).isEqualTo(5000L);
        assertThat(response.nativeScore()).isEqualTo(82);
        assertThat(response.nativeLevelLabel()).isEqualTo("유학생 수준");
        assertThat(response.turnFeedbacks()).hasSize(1);
        assertThat(response.turnFeedbacks().getFirst().feedbackType()).isEqualTo(FeedbackType.GOOD);
    }

    @Test
    void rejectsSessionFeedbackWithoutRequiredQualityFields() throws Exception {
        RemoteAiConversationClient client = clientWithResponse(
                "/api/v1/conversation/session-feedback",
                new AtomicReference<>(),
                new AtomicReference<>(),
                """
                        {
                          "sessionId":1000,
                          "nativeScore":82,
                          "turnFeedbacks":[]
                        }
                        """);

        assertThatThrownBy(() -> client.generateSessionFeedback(sessionFeedbackRequest()))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    private RemoteAiConversationClient clientWithResponse(
            String path,
            AtomicReference<String> requestBody,
            AtomicReference<String> requestIdHeader,
            String responseBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(path, exchange -> {
            requestIdHeader.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.start();

        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        AiClientProperties properties = new AiClientProperties(
                baseUrl,
                "remote",
                "/api/v1/conversation/next-question",
                "/api/v1/conversation/turn-feedback",
                "/api/v1/conversation/session-feedback",
                Duration.ofSeconds(180));
        return new RemoteAiConversationClient(objectMapper, properties);
    }

    private AiNextQuestionRequest nextQuestionRequest() {
        return new AiNextQuestionRequest(
                1000L,
                5000L,
                1,
                scenarioContext(),
                new AiTurnContext(
                        "What is your favorite food? Why do you like it?",
                        "가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?",
                        "I like pizza because it is spicy."),
                new AiFixedQuestion(101L, 2, "Do you cook often?", "요리는 자주 하나요?"));
    }

    private AiTurnFeedbackRequest turnFeedbackRequest() {
        return new AiTurnFeedbackRequest(
                1000L,
                5000L,
                1,
                scenarioContext(),
                new AiTurnContext(
                        "What is your favorite food? Why do you like it?",
                        "가장 좋아하는 음식이 뭐예요? 왜 좋아하나요?",
                        "I like pizza because it is spicy."));
    }

    private AiSessionFeedbackRequest sessionFeedbackRequest() {
        return new AiSessionFeedbackRequest(
                1000L,
                scenarioContext(),
                List.of(5000L));
    }

    private AiScenarioContext scenarioContext() {
        return new AiScenarioContext(
                10L,
                "음식 취향 이야기하기",
                "좋아하는 음식과 최근 먹었던 음식에 대해 이야기합니다.",
                "음식 취향과 경험을 영어로 자연스럽게 설명할 수 있다.");
    }
}
