// Bearer access token을 검증해 SecurityContext에 인증 주체를 저장하는 필터
package com.saynow.auth.security;

import com.saynow.auth.application.SaynowTokenService;
import com.saynow.auth.infrastructure.UserRepository;
import com.saynow.common.exception.ApiException;
import com.saynow.common.exception.ErrorCode;
import com.saynow.common.observability.RequestTraceContext;
import com.saynow.common.web.RequestLoggingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SaynowTokenService saynowTokenService;
    private final UserRepository userRepository;
    private final UserActiveStatusCache userActiveStatusCache;
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

        long stageStartedAt = System.nanoTime();
        Long userId;
        try {
            userId = saynowTokenService.parseAccessToken(authorization.substring(BEARER_PREFIX.length()));
        } catch (ApiException exception) {
            logStageLatency(request, null, stageStartedAt);
            failureResponseWriter.write(response, exception.getErrorCode());
            return;
        }
        if (!userActiveStatusCache.isActive(userId, candidate -> userRepository.existsByIdAndDeletedAtIsNull(candidate))) {
            logStageLatency(request, userId, stageStartedAt);
            failureResponseWriter.write(response, ErrorCode.AUTH_REQUIRED);
            return;
        }
        logStageLatency(request, userId, stageStartedAt);

        request.setAttribute(RequestLoggingFilter.USER_ID_ATTRIBUTE, userId);
        MDC.put("userId", String.valueOf(userId));
        AuthUserPrincipal principal = new AuthUserPrincipal(userId);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(securityContext);
        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            MDC.remove("userId");
        }
    }

    private void logStageLatency(HttpServletRequest request, Long userId, long startedAtNanos) {
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
        log.info(
                "event=be_stage_latency requestId={} workflow=http_request stage=auth_token elapsedMs={} sessionId={} userId={}",
                RequestTraceContext.currentRequestId().orElse("none"),
                elapsedMs,
                sessionId(request),
                userId == null ? "anonymous" : userId);
    }

    private String sessionId(HttpServletRequest request) {
        String path = request.getRequestURI();
        String sessionPrefix = "/api/v1/sessions/";
        if (!path.startsWith(sessionPrefix)) {
            return "none";
        }
        String tail = path.substring(sessionPrefix.length());
        int slashIndex = tail.indexOf('/');
        String sessionId = slashIndex < 0 ? tail : tail.substring(0, slashIndex);
        return sessionId.isBlank() ? "none" : sessionId;
    }
}
