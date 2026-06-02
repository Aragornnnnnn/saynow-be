// dev 프로필의 OpenAPI 서버 URL이 현재 active dev 도메인을 사용하는지 검증한다.
package com.saynow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("dev")
@TestPropertySource(properties = "saynow.openapi.server-url=https://dev-saynow.p-e.kr")
class DevOpenApiIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devProfileExposesActiveDevelopServerUrlEvenWhenLegacyDevUrlIsInjected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("https://saynow.p-e.kr"));
    }
}
