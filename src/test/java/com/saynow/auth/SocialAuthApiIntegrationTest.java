// 소셜 로그인 인증 API의 통합 동작을 검증하는 테스트
package com.saynow.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocialAuthApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void socialLoginCreatesMemberAndReturnsTokensWithoutProfileImage() throws Exception {
        MvcResult firstLogin = mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"test-google-sub-1|ryan@example.com|Ryan",
                                  "nonce":"nonce-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(1800))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600))
                .andExpect(jsonPath("$.data.member.memberId").isNotEmpty())
                .andExpect(jsonPath("$.data.member.nickname").value("Ryan"))
                .andExpect(jsonPath("$.data.member.email").value("ryan@example.com"))
                .andExpect(jsonPath("$.data.member.provider").value("GOOGLE"))
                .andExpect(jsonPath("$.data.member.newMember").value(true))
                .andExpect(jsonPath("$.data.member.profileImageUrl").doesNotExist())
                .andReturn();

        JsonNode firstBody = objectMapper.readTree(firstLogin.getResponse().getContentAsByteArray());
        String memberId = firstBody.get("data").get("member").get("memberId").asText();

        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"test-google-sub-1|ryan@example.com|Ryan",
                                  "nonce":"nonce-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.memberId").value(memberId))
                .andExpect(jsonPath("$.data.member.newMember").value(false));
    }

    @Test
    void refreshRotatesTokenAndLogoutRevokesIt() throws Exception {
        JsonNode loginBody = login("test-google-sub-2|refresh@example.com|Refresh User");
        String refreshToken = loginBody.get("data").get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken", not(refreshToken)))
                .andReturn();

        JsonNode refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsByteArray());
        String rotatedRefreshToken = refreshBody.get("data").get("refreshToken").asText();
        assertThat(rotatedRefreshToken).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));
    }

    @Test
    void socialLoginKeepsExistingProfileWhenProviderOmitsOptionalClaims() throws Exception {
        JsonNode firstLogin = login("test-google-sub-3|keep@example.com|Keep User");
        String memberId = firstLogin.get("data").get("member").get("memberId").asText();

        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"test-google-sub-3||",
                                  "nonce":"nonce-2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.member.memberId").value(memberId))
                .andExpect(jsonPath("$.data.member.nickname").value("Keep User"))
                .andExpect(jsonPath("$.data.member.email").value("keep@example.com"))
                .andExpect(jsonPath("$.data.member.newMember").value(false));
    }

    @Test
    void withdrawRevokesTokensRejectsExistingAccessTokenAndAllowsFreshSocialSignup() throws Exception {
        JsonNode loginBody = login("withdraw-sub|withdraw@example.com|Withdraw User");
        String accessToken = loginBody.get("data").get("accessToken").asText();
        String refreshToken = loginBody.get("data").get("refreshToken").asText();
        String memberId = loginBody.get("data").get("member").get("memberId").asText();

        mockMvc.perform(delete("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(post("/api/v1/auth/token/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("REFRESH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/v1/sessions")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"scenarioId":"cafe_iced_americano"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));

        JsonNode reloginBody = login("withdraw-sub|withdraw@example.com|Withdraw User");
        assertThat(reloginBody.get("data").get("member").get("newMember").asBoolean()).isTrue();
        assertThat(reloginBody.get("data").get("member").get("memberId").asText()).isNotEqualTo(memberId);
    }

    private JsonNode login(String idToken) throws Exception {
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

        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }
}
