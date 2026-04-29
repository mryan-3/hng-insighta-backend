package com.hng.stage3.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(2)
public class RateLimitingFilter implements Filter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String clientIp = httpRequest.getRemoteAddr();
        
        Bucket bucket;
        if (path.startsWith("/auth/")) {
            bucket = authBuckets.computeIfAbsent(clientIp, k -> createNewBucket(10, 1));
        } else {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String key = (auth != null && auth.isAuthenticated()) ? auth.getName() : clientIp;
            bucket = apiBuckets.computeIfAbsent(key, k -> createNewBucket(60, 1));
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"status\": \"error\", \"message\": \"Too Many Requests\"}");
        }
    }

    private Bucket createNewBucket(int capacity, int minutes) {
        return Bucket4j.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofMinutes(minutes))))
                .build();
    }
}
