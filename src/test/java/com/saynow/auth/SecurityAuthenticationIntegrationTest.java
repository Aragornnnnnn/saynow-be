// Spring Security 기반 인증 구성이 등록됐는지 검증하는 테스트
package com.saynow.auth;

import com.saynow.IntegrationTestSupport;
import com.saynow.auth.security.AuthMemberPrincipal;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.SentryEventReporter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAuthenticationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CapturingSentryEventReporter sentryEventReporter;

    @BeforeEach
    void clearCapturedEvents() {
        sentryEventReporter.clear();
    }

    @Test
    void authenticatesWithOncePerRequestFilterInsteadOfMvcInterceptor() {
        assertThat(applicationContext.getBeansOfType(OncePerRequestFilter.class))
                .containsKey("authTokenFilter");
        assertThat(applicationContext.getBeansOfType(HandlerInterceptor.class))
                .doesNotContainKey("authInterceptor");
        assertThat(UserDetails.class).isAssignableFrom(AuthMemberPrincipal.class);
    }

    @Test
    void capturesAuthenticationFailureWithSentry() throws Exception {
        mockMvc.perform(post("/api/v1/sessions"))
                .andExpect(status().isUnauthorized());

        assertThat(sentryEventReporter.capturedExceptions)
                .singleElement()
                .isInstanceOfSatisfying(ApiException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_REQUIRED));
    }

    @Test
    void allowsProductionFrontendOriginForCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header(HttpHeaders.ORIGIN, "https://saynow-fe-web.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://saynow-fe-web.vercel.app"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void allowsLocalhostFrontendOriginForCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void allowsLocalNetworkFrontendOriginForCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header(HttpHeaders.ORIGIN, "http://172.30.1.89:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://172.30.1.89:3000"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void allowsAndroidEmulatorFrontendOriginForCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/sessions")
                        .header(HttpHeaders.ORIGIN, "http://10.0.2.2:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://10.0.2.2:3000"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @TestConfiguration
    static class SentryTestConfiguration {

        @Bean
        @Primary
        CapturingSentryEventReporter capturingSentryEventReporter() {
            return new CapturingSentryEventReporter();
        }
    }

    static class CapturingSentryEventReporter implements SentryEventReporter {

        private final List<Throwable> capturedExceptions = new ArrayList<>();

        @Override
        public void captureException(Throwable exception) {
            capturedExceptions.add(exception);
        }

        void clear() {
            capturedExceptions.clear();
        }
    }
}
