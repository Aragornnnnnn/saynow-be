// 원격 AI 꼬리 질문 응답의 분류값 매핑을 검증한다.
package com.saynow.session.infrastructure.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteAiConversationClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void mapsNextQuestionTurnClassification() throws Exception {
        RemoteAiConversationClient client = clientWithNextQuestionResponse("""
                {
                  "nextQuestion":"The menu includes iced Americano, latte, cappuccino, and tea. What would you like to order?",
                  "translatedQuestion":"메뉴에는 아이스 아메리카노, 라떼, 카푸치노, 차가 있어요. 무엇을 주문하시겠어요?",
                  "filledSlots":[],
                  "turnClassification":"ASSISTANCE_REQUEST"
                }
                """);

        AiNextQuestionResponse response = client.generateNextQuestion(nextQuestionRequest());

        assertThat(response.nextQuestion()).contains("The menu includes iced Americano");
        assertThat(response.filledSlots()).isEmpty();
        assertThat(response.turnClassification()).isEqualTo(TurnClassification.ASSISTANCE_REQUEST);
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

    private RemoteAiConversationClient clientWithNextQuestionResponse(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/conversation/next-question", exchange -> {
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
                Duration.ofSeconds(180));
        return new RemoteAiConversationClient(new ObjectMapper(), properties, WebClient.builder());
    }

    private AiNextQuestionRequest nextQuestionRequest() {
        return new AiNextQuestionRequest(
                "What would you like to order?",
                "Can I see the menu?",
                "카페에서 주문하기",
                "원하는 음료를 자연스럽게 주문할 수 있다.",
                List.of(new AiSlotStatus("drink", false)));
    }
}
