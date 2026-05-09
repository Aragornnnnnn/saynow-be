// Spring Security 기반 인증 구성이 등록됐는지 검증하는 테스트
package com.saynow.auth;

import com.saynow.IntegrationTestSupport;
import com.saynow.auth.security.AuthMemberPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityAuthenticationIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void authenticatesWithOncePerRequestFilterInsteadOfMvcInterceptor() {
        assertThat(applicationContext.getBeansOfType(OncePerRequestFilter.class))
                .containsKey("authTokenFilter");
        assertThat(applicationContext.getBeansOfType(HandlerInterceptor.class))
                .doesNotContainKey("authInterceptor");
        assertThat(UserDetails.class).isAssignableFrom(AuthMemberPrincipal.class);
    }
}
