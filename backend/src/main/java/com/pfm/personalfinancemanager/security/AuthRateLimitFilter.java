package com.pfm.personalfinancemanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pfm.personalfinancemanager.dto.response.ApiResponse;
import com.pfm.personalfinancemanager.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;

/**
 * Protects public authentication endpoints against brute-force and credential-stuffing bursts.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_AUTH_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    );

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.max-requests:20}")
    private int maxRequests;

    @Value("${app.security.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Value("${app.security.rate-limit.trust-forwarded-headers:false}")
    private boolean trustForwardedHeaders;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        if (!enabled || !HttpMethod.POST.matches(request.getMethod())) {
            return true;
        }
        String uri = request.getRequestURI();
        return !PROTECTED_AUTH_PATHS.contains(uri);
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        String bucketKey = clientIp + ":" + request.getRequestURI();

        RateLimiterService.RateLimitResult decision = rateLimiterService.consume(
                bucketKey,
                Math.max(1, maxRequests),
                Duration.ofSeconds(Math.max(1L, windowSeconds))
        );

        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (!decision.allowed()) {
            ErrorCode errorCode = ErrorCode.RATE_LIMIT_EXCEEDED;
            response.setStatus(errorCode.getHttpStatus().value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            ApiResponse<Void> body = ApiResponse.error(errorCode.getCode(), errorCode.getDefaultMessage());
            objectMapper.writeValue(response.getWriter(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
        }
        return StringUtils.hasText(request.getRemoteAddr()) ? request.getRemoteAddr() : "unknown";
    }
}

