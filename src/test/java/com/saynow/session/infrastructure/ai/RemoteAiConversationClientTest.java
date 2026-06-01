// 원격 AI 꼬리 질문 응답의 분류값 매핑을 검증한다.
package com.saynow.session.infrastructure.ai;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteAiConversationClientTest {

    private HttpServer server;
    private final List<AttachedAppender> attachedAppenders = new ArrayList<>();

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        attachedAppenders.forEach(AttachedAppender::detach);
        attachedAppenders.clear();
        RequestTraceContext.clear();
    }

    @Test
    void mapsNextQuestionTurnClassification() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        ListAppender<ILoggingEvent> aiClientLogs = attachListAppender();
        RemoteAiConversationClient client = clientWithNextQuestionResponse(requestBody, requestIdHeader, """
                {
                  "nextQuestion":"The menu includes iced Americano, latte, cappuccino, and tea. What would you like to order?",
                  "translatedQuestion":"메뉴에는 아이스 아메리카노, 라떼, 카푸치노, 차가 있어요. 무엇을 주문하시겠어요?",
                  "nextQuestionTargetSlotName":"drink",
                  "filledSlots":[],
                  "turnClassification":"ASSISTANCE_REQUEST"
                }
                """);
        RequestTraceContext.start("trace-next-question-123");

        AiNextQuestionResponse response = client.generateNextQuestion(nextQuestionRequest());

        assertThat(requestIdHeader.get()).isEqualTo("trace-next-question-123");
        assertThat(aiClientLogs.list)
                .anySatisfy(event -> assertThat(event.getFormattedMessage())
                        .contains("event=ai_call_latency")
                        .contains("requestId=trace-next-question-123")
                        .contains("workflow=next_question")
                        .contains("status=200")
                        .containsPattern("elapsedMs=\\d+"));
        assertThat(response.nextQuestion()).contains("The menu includes iced Americano");
        assertThat(response.nextQuestionTargetSlotName()).isEqualTo("drink");
        assertThat(response.filledSlots()).isEmpty();
        assertThat(response.turnClassification()).isEqualTo(TurnClassification.ASSISTANCE_REQUEST);
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("originalQuestionTargetSlotName").asText())
                .isEqualTo("drink");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("originalTranslatedQuestion").asText())
                .isEqualTo("어떤 음료를 주문하고 싶으신가요?");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("aiRole").asText())
                .isEqualTo("카페 직원");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("scenarioSituation").asText())
                .isEqualTo("카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).get("description").asText())
                .isEqualTo("사용자가 주문하려는 음료 이름이나 종류를 구체적으로 말했는지 여부");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).get("evidencePolicy").isObject())
                .isTrue();
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).get("evidencePolicy").get("mode").asText())
                .isEqualTo("semantic_evidence");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).get("evidencePolicy").get("requiresEvidenceText").asBoolean())
                .isTrue();
    }

    @Test
    void mapsRepeatRequestTurnClassification() throws Exception {
        RemoteAiConversationClient client = clientWithNextQuestionResponse("""
                {
                  "nextQuestion":"What drink would you like to order?",
                  "translatedQuestion":"어떤 음료를 주문하고 싶으신가요?",
                  "nextQuestionTargetSlotName":"drink",
                  "filledSlots":[],
                  "turnClassification":"REPEAT_REQUEST"
                }
                """);

        AiNextQuestionResponse response = client.generateNextQuestion(nextQuestionRequest());

        assertThat(response.nextQuestion()).isEqualTo("What drink would you like to order?");
        assertThat(response.nextQuestionTargetSlotName()).isEqualTo("drink");
        assertThat(response.filledSlots()).isEmpty();
        assertThat(response.turnClassification()).isEqualTo(TurnClassification.REPEAT_REQUEST);
    }

    @Test
    void rejectsNextQuestionResponseWithoutTurnClassification() throws Exception {
        RemoteAiConversationClient client = clientWithNextQuestionResponse("""
                {
                  "nextQuestion":"What drink would you like to order?",
                  "translatedQuestion":"어떤 음료를 주문하고 싶으신가요?",
                  "filledSlots":[]
                }
                """);

        assertThatThrownBy(() -> client.generateNextQuestion(nextQuestionRequest()))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsNextQuestionResponseWithUnknownTurnClassification() throws Exception {
        RemoteAiConversationClient client = clientWithNextQuestionResponse("""
                {
                  "nextQuestion":"What drink would you like to order?",
                  "translatedQuestion":"어떤 음료를 주문하고 싶으신가요?",
                  "filledSlots":[],
                  "turnClassification":"OFF_TOPIC"
                }
                """);

        assertThatThrownBy(() -> client.generateNextQuestion(nextQuestionRequest()))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void feedbackRequestIncludesSessionResultScenarioSituationAndSlots() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        RemoteAiConversationClient client = clientWithFeedbackResponse(requestBody, requestIdHeader, """
                {
                  "comprehensionScore":82,
                  "feedbackSummary":"전체적으로 의도는 잘 전달됐습니다.",
                  "turnFeedbacks":[
                    {
                      "turnId":101,
                      "feedbackRequired":true,
                      "nativeUnderstanding":"의도를 이해했습니다.",
                      "nativeLanguageInterpretation":"자연스럽게 들립니다.",
                      "betterExpression":"I'd like an iced Americano, please."
                    }
                  ]
                }
                """);
        RequestTraceContext.start("trace-feedback-123");

        client.generateFeedback(feedbackRequest());

        assertThat(requestIdHeader.get()).isEqualTo("trace-feedback-123");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("sessionResult").asText())
                .isEqualTo("SUCCESS");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("aiRole").asText())
                .isEqualTo("카페 직원");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("scenarioSituation").asText())
                .isEqualTo("카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).get("description").asText())
                .isEqualTo("사용자가 주문하려는 음료 이름이나 종류를 구체적으로 말했는지 여부");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).get("filled").asBoolean())
                .isTrue();
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("slots").get(0).has("evidencePolicy"))
                .isFalse();
    }

    @Test
    void guideRequestIncludesScenarioContextAndMapsAnswer() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        RemoteAiConversationClient client = clientWithGuideResponse(requestBody, requestIdHeader, """
                {
                  "answer":"would는 공손한 요청이나 가정 느낌을 줄 때 써요."
                }
                """);
        RequestTraceContext.start("trace-guide-123");

        AiGuideResponse response = client.generateGuide(new AiGuideRequest(
                "I would like coffee에서 would는 왜 쓰나요?",
                "카페에서 주문하기",
                "원하는 음료를 자연스럽게 주문할 수 있다.",
                "카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.",
                "카페 직원"));

        assertThat(requestIdHeader.get()).isEqualTo("trace-guide-123");
        assertThat(response.answer()).isEqualTo("would는 공손한 요청이나 가정 느낌을 줄 때 써요.");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("question").asText())
                .isEqualTo("I would like coffee에서 would는 왜 쓰나요?");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("scenarioTitle").asText())
                .isEqualTo("카페에서 주문하기");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("scenarioGoal").asText())
                .isEqualTo("원하는 음료를 자연스럽게 주문할 수 있다.");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("scenarioSituation").asText())
                .isEqualTo("카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.");
        assertThat(new ObjectMapper().readTree(requestBody.get()).get("aiRole").asText())
                .isEqualTo("카페 직원");
    }

    @Test
    void rejectsGuideResponseWithoutAnswer() throws Exception {
        RemoteAiConversationClient client = clientWithGuideResponse(new AtomicReference<>(), """
                {
                  "message":"missing answer"
                }
                """);

        assertThatThrownBy(() -> client.generateGuide(new AiGuideRequest(
                        "would는 왜 쓰나요?",
                        "카페에서 주문하기",
                        "원하는 음료를 자연스럽게 주문할 수 있다.",
                        "카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.",
                        "카페 직원")))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.AI_GENERATION_FAILED);
    }

    @Test
    void feedbackStreamRequestIncludesRequestIdHeader() throws Exception {
        AtomicReference<String> requestIdHeader = new AtomicReference<>();
        RemoteAiConversationClient client = clientWithFeedbackStreamResponse(requestIdHeader, """
                event: summary
                data: {"comprehensionScore":82,"feedbackSummary":"전체적으로 의도는 잘 전달됐습니다."}

                event: done
                data: {"turnCount":1}

                """);
        RequestTraceContext.start("trace-feedback-stream-123");

        List<AiFeedbackStreamEvent> events = client.streamFeedback(feedbackRequest())
                .collectList()
                .block(Duration.ofSeconds(3));

        assertThat(requestIdHeader.get()).isEqualTo("trace-feedback-stream-123");
        assertThat(events)
                .extracting(AiFeedbackStreamEvent::event)
                .containsExactly("summary", "done");
    }

    private RemoteAiConversationClient clientWithNextQuestionResponse(String responseBody) throws IOException {
        return clientWithNextQuestionResponse(new AtomicReference<>(), responseBody);
    }

    private ListAppender<ILoggingEvent> attachListAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(RemoteAiConversationClient.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        attachedAppenders.add(new AttachedAppender(logger, appender));
        return appender;
    }

    private record AttachedAppender(Logger logger, ListAppender<ILoggingEvent> appender) {

        private void detach() {
            logger.detachAppender(appender);
        }
    }

    private RemoteAiConversationClient clientWithNextQuestionResponse(
            AtomicReference<String> requestBody,
            String responseBody
    ) throws IOException {
        return clientWithNextQuestionResponse(requestBody, new AtomicReference<>(), responseBody);
    }

    private RemoteAiConversationClient clientWithNextQuestionResponse(
            AtomicReference<String> requestBody,
            AtomicReference<String> requestIdHeader,
            String responseBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/conversation/next-question", exchange -> {
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
                "/api/v1/conversation/feedback",
                "/api/v1/conversation/feedback/stream",
                "/api/v1/conversation/guide",
                Duration.ofSeconds(180));
        return new RemoteAiConversationClient(new ObjectMapper(), properties, WebClient.builder());
    }

    private RemoteAiConversationClient clientWithFeedbackResponse(
            AtomicReference<String> requestBody,
            String responseBody
    ) throws IOException {
        return clientWithFeedbackResponse(requestBody, new AtomicReference<>(), responseBody);
    }

    private RemoteAiConversationClient clientWithFeedbackResponse(
            AtomicReference<String> requestBody,
            AtomicReference<String> requestIdHeader,
            String responseBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/conversation/feedback", exchange -> {
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
                "/api/v1/conversation/feedback",
                "/api/v1/conversation/feedback/stream",
                "/api/v1/conversation/guide",
                Duration.ofSeconds(180));
        return new RemoteAiConversationClient(new ObjectMapper(), properties, WebClient.builder());
    }

    private RemoteAiConversationClient clientWithGuideResponse(
            AtomicReference<String> requestBody,
            String responseBody
    ) throws IOException {
        return clientWithGuideResponse(requestBody, new AtomicReference<>(), responseBody);
    }

    private RemoteAiConversationClient clientWithGuideResponse(
            AtomicReference<String> requestBody,
            AtomicReference<String> requestIdHeader,
            String responseBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/conversation/guide", exchange -> {
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
                "/api/v1/conversation/feedback",
                "/api/v1/conversation/feedback/stream",
                "/api/v1/conversation/guide",
                Duration.ofSeconds(180));
        return new RemoteAiConversationClient(new ObjectMapper(), properties, WebClient.builder());
    }

    private RemoteAiConversationClient clientWithFeedbackStreamResponse(
            AtomicReference<String> requestIdHeader,
            String responseBody
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/conversation/feedback/stream", exchange -> {
            requestIdHeader.set(exchange.getRequestHeaders().getFirst("X-Request-Id"));
            byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
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
                "/api/v1/conversation/feedback",
                "/api/v1/conversation/feedback/stream",
                "/api/v1/conversation/guide",
                Duration.ofSeconds(180));
        return new RemoteAiConversationClient(new ObjectMapper(), properties, WebClient.builder());
    }

    private AiNextQuestionRequest nextQuestionRequest() {
        return new AiNextQuestionRequest(
                "What would you like to order?",
                "어떤 음료를 주문하고 싶으신가요?",
                "drink",
                "Can I see the menu?",
                "카페에서 주문하기",
                "카페 직원",
                "카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.",
                "원하는 음료를 자연스럽게 주문할 수 있다.",
                List.of(new AiNextQuestionSlotStatus(
                        "drink",
                        "사용자가 주문하려는 음료 이름이나 종류를 구체적으로 말했는지 여부",
                        false,
                        new AiSlotEvidencePolicy(
                                "semantic_evidence",
                                List.of("iced americano", "latte"),
                                true,
                                "latest_user_utterance"))));
    }

    private AiFeedbackRequest feedbackRequest() {
        return new AiFeedbackRequest(
                "카페에서 주문하기",
                "카페 직원",
                "카페에서 직원에게 메뉴를 확인하고 음료를 주문하는 상황입니다.",
                "원하는 음료를 자연스럽게 주문할 수 있다.",
                "SUCCESS",
                List.of(new AiSlotStatus(
                        "drink",
                        "사용자가 주문하려는 음료 이름이나 종류를 구체적으로 말했는지 여부",
                        true)),
                List.of(new AiFeedbackTurnRequest(
                        101L,
                        "What would you like to order?",
                        "I want an iced americano.")));
    }
}
