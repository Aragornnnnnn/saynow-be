// 연습 세션 API의 인증과 소유자 검증을 확인하는 테스트
package com.saynow.practice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PracticeSessionAuthorizationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsSessionStartWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"cafe_iced_americano"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
    }

    @Test
    void storesSessionOwnerAndRejectsOtherMemberAccess() throws Exception {
        String ownerAccessToken = loginAccessToken("owner-sub|owner@example.com|Owner");
        String otherAccessToken = loginAccessToken("other-sub|other@example.com|Other");

        MvcResult started = mockMvc.perform(post("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerAccessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"cafe_iced_americano"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.scenarioId").value("cafe_iced_americano"))
                .andReturn();

        JsonNode startBody = objectMapper.readTree(started.getResponse().getContentAsByteArray());
        String sessionId = startBody.get("data").get("sessionId").asText();

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ownerAccessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}", sessionId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(otherAccessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SESSION_ACCESS_DENIED"));
    }

    private String loginAccessToken(String idToken) throws Exception {
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
