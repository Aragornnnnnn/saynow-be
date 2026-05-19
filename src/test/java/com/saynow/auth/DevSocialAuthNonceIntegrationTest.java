// dev 환경의 소셜 로그인 nonce 검증 비활성화 설정을 검증한다.
package com.saynow.auth;

import com.saynow.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "saynow.auth.oidc.nonce-required=false")
class DevSocialAuthNonceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void socialLoginAllowsMissingNonceWhenNonceRequiredIsFalse() throws Exception {
        mockMvc.perform(post("/api/v1/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider":"GOOGLE",
                                  "idToken":"dev-no-nonce-sub|dev-nonce@example.com|Dev Nonce"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value("dev-nonce@example.com"));
    }
}
