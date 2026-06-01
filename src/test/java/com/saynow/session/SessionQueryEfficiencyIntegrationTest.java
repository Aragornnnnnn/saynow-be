// 세션 발화 처리의 DB 왕복 수가 예산 안에 있는지 검증하는 통합 테스트
package com.saynow.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SessionQueryEfficiencyIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Statistics statistics;

    @BeforeEach
    void setUpStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
    }

    @Test
    void submitUtteranceUsesBoundedDatabaseStatements() throws Exception {
        String accessToken = login("query-budget-sub|query-budget@example.com|Query Budget User");
        long sessionId = startSession(accessToken, 4);

        statistics.clear();
        mockMvc.perform(post("/api/v1/sessions/{sessionId}/utterances", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userUtterance":"I'm here for sightseeing."}
                                """))
                .andExpect(status().isOk());

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(6);
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
}
