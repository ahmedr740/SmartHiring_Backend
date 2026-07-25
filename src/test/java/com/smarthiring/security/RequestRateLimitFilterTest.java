package com.smarthiring.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimitFilterTest {

    @Test
    void rejectsRequestsBeyondConfiguredLimit() throws Exception {
        RequestRateLimitFilter.Policy twoPerMinute = new RequestRateLimitFilter.Policy("login", 2, 60);
        RequestRateLimitFilter filter = new RequestRateLimitFilter(
                true,
                twoPerMinute,
                new RequestRateLimitFilter.Policy("register", 5, 60),
                new RequestRateLimitFilter.Policy("matching", 5, 60),
                new RequestRateLimitFilter.Policy("applications", 5, 60),
                new RequestRateLimitFilter.Policy("chat", 5, 60),
                new RequestRateLimitFilter.Policy("shiftDraft", 5, 60),
                Clock.fixed(Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC)
        );

        assertThat(runLoginRequest(filter).getStatus()).isEqualTo(200);
        assertThat(runLoginRequest(filter).getStatus()).isEqualTo(200);

        MockHttpServletResponse rejected = runLoginRequest(filter);
        assertThat(rejected.getStatus()).isEqualTo(429);
        assertThat(rejected.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rejected.getContentAsString()).contains("Too many requests");
    }

    private MockHttpServletResponse runLoginRequest(RequestRateLimitFilter filter) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr("203.0.113.10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }
}
