// HTTP 요청 단위 추적 로그와 MDC requestId를 관리하는 필터
package com.saynow.common.web;

import com.saynow.common.observability.RequestTraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String USER_ID_ATTRIBUTE = RequestLoggingFilter.class.getName() + ".userId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = requestId(request);
        RequestTraceContext.start(requestId);
        response.setHeader(RequestTraceContext.REQUEST_ID_HEADER, requestId);

        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long totalMs = (System.nanoTime() - startedAt) / 1_000_000;
            long aiCallMs = RequestTraceContext.aiCallMs();
            log.info(
                    "event=api_latency requestId={} method={} path={} sessionId={} status={} totalMs={} aiCallMs={} userId={}",
                    requestId,
                    request.getMethod(),
                    request.getRequestURI(),
                    sessionId(request),
                    response.getStatus(),
                    totalMs,
                    aiCallMs,
                    userId(request));
            RequestTraceContext.clear();
        }
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader(RequestTraceContext.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
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

    private String userId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        return userId == null ? "anonymous" : userId.toString();
    }
}
