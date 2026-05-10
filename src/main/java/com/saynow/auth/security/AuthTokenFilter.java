// Bearer access token을 검증해 SecurityContext에 인증 주체를 저장하는 필터
package com.saynow.auth.security;

import com.saynow.auth.application.SaynowTokenService;
import com.saynow.common.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SaynowTokenService saynowTokenService;
    private final AuthFailureResponseWriter failureResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long memberId;
        try {
            memberId = saynowTokenService.parseAccessToken(authorization.substring(BEARER_PREFIX.length()));
        } catch (ApiException exception) {
            failureResponseWriter.write(response, exception.getErrorCode());
            return;
        }

        AuthMemberPrincipal principal = new AuthMemberPrincipal(memberId);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
