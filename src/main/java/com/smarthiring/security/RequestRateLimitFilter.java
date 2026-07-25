package com.smarthiring.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();
    private final boolean enabled;
    private final Policy loginPolicy;
    private final Policy registerPolicy;
    private final Policy matchingPolicy;
    private final Policy applicationPolicy;
    private final Policy chatPolicy;
    private final Policy shiftDraftPolicy;
    private final Clock clock;

    @Autowired
    public RequestRateLimitFilter(
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.login.requests:10}") int loginRequests,
            @Value("${app.rate-limit.login.window-seconds:300}") long loginWindowSeconds,
            @Value("${app.rate-limit.register.requests:5}") int registerRequests,
            @Value("${app.rate-limit.register.window-seconds:600}") long registerWindowSeconds,
            @Value("${app.rate-limit.matching.requests:60}") int matchingRequests,
            @Value("${app.rate-limit.matching.window-seconds:3600}") long matchingWindowSeconds,
            @Value("${app.rate-limit.applications.requests:30}") int applicationRequests,
            @Value("${app.rate-limit.applications.window-seconds:3600}") long applicationWindowSeconds,
            @Value("${app.rate-limit.chat.requests:60}") int chatRequests,
            @Value("${app.rate-limit.chat.window-seconds:60}") long chatWindowSeconds,
            @Value("${app.rate-limit.shift-draft.requests:20}") int shiftDraftRequests,
            @Value("${app.rate-limit.shift-draft.window-seconds:3600}") long shiftDraftWindowSeconds
    ) {
        this(
                enabled,
                new Policy("login", loginRequests, loginWindowSeconds),
                new Policy("register", registerRequests, registerWindowSeconds),
                new Policy("matching", matchingRequests, matchingWindowSeconds),
                new Policy("applications", applicationRequests, applicationWindowSeconds),
                new Policy("chat", chatRequests, chatWindowSeconds),
                new Policy("shiftDraft", shiftDraftRequests, shiftDraftWindowSeconds),
                Clock.systemUTC()
        );
    }

    RequestRateLimitFilter(
            boolean enabled,
            Policy loginPolicy,
            Policy registerPolicy,
            Policy matchingPolicy,
            Policy applicationPolicy,
            Policy chatPolicy,
            Policy shiftDraftPolicy,
            Clock clock
    ) {
        this.enabled = enabled;
        this.loginPolicy = loginPolicy;
        this.registerPolicy = registerPolicy;
        this.matchingPolicy = matchingPolicy;
        this.applicationPolicy = applicationPolicy;
        this.chatPolicy = chatPolicy;
        this.shiftDraftPolicy = shiftDraftPolicy;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || "OPTIONS".equalsIgnoreCase(request.getMethod()) || policyFor(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Policy policy = policyFor(request);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = clock.millis();
        String key = policy.name() + ":" + clientIdentity(request);
        WindowCounter counter = counters.compute(key, (ignored, current) -> {
            if (current == null || now >= current.windowEndsAtMillis()) {
                return new WindowCounter(1, now + policy.windowSeconds() * 1000L);
            }
            return new WindowCounter(current.count() + 1, current.windowEndsAtMillis());
        });

        if (requestCount.incrementAndGet() % 1000 == 0) {
            counters.entrySet().removeIf(entry -> now >= entry.getValue().windowEndsAtMillis());
        }

        int remaining = Math.max(0, policy.maxRequests() - counter.count());
        response.setHeader("X-RateLimit-Limit", Integer.toString(policy.maxRequests()));
        response.setHeader("X-RateLimit-Remaining", Integer.toString(remaining));

        if (counter.count() > policy.maxRequests()) {
            long retryAfter = Math.max(1, (counter.windowEndsAtMillis() - now + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Policy policyFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && "/api/auth/login".equals(path)) {
            return loginPolicy;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/auth/register".equals(path)) {
            return registerPolicy;
        }
        if (path.startsWith("/api/matches/")) {
            return matchingPolicy;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/applications".equals(path)) {
            return applicationPolicy;
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/chats/")) {
            return chatPolicy;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/shifts/ai-draft".equals(path)) {
            return shiftDraftPolicy;
        }
        return null;
    }

    private String clientIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName().toLowerCase();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        String address = forwardedFor == null || forwardedFor.isBlank()
                ? request.getRemoteAddr()
                : forwardedFor.split(",", 2)[0].trim();
        return "ip:" + address;
    }

    record Policy(String name, int maxRequests, long windowSeconds) {
        Policy {
            if (maxRequests < 1 || windowSeconds < 1) {
                throw new IllegalArgumentException("Rate limit values must be positive");
            }
        }
    }

    private record WindowCounter(int count, long windowEndsAtMillis) {
    }
}
