package com.dbwb.platform.security;

import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Nothing capped these endpoints before. Login, registration and password
 * reset were unbounded, so credential stuffing was limited only by bandwidth;
 * the public endpoints are anonymous and write an analytics row per call; and
 * the AI endpoint spends real money at OpenRouter on every request.
 */
class RateLimitFilterTest {

    private RateLimitProperties properties;
    private RateLimiter rateLimiter;
    private RateLimitFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        properties = new RateLimitProperties();
        properties.setLogin(new RateLimitProperties.Policy(3, 15));
        properties.setAiSuggestions(new RateLimitProperties.Policy(2, 60));
        rateLimiter = new RateLimiter();
        filter = new RateLimitFilter(properties, rateLimiter, new ObjectMapper());
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refusesLoginAttemptsOnceTheAllowanceIsSpent() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(login("203.0.113.10").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse refused = login("203.0.113.10");

        assertThat(refused.getStatus()).isEqualTo(429);
        assertThat(refused.getContentAsString()).contains("Too many attempts");
        assertThat(refused.getHeader("Retry-After")).isEqualTo("900");
        // The refused attempt must never reach AuthService.
        verify(chain, times(3)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void countsEachClientAddressSeparately() throws Exception {
        for (int i = 0; i < 3; i++) {
            login("203.0.113.10");
        }

        // One attacker exhausting their allowance must not lock everyone else out.
        assertThat(login("203.0.113.99").getStatus()).isEqualTo(200);
    }

    @Test
    void countsAiCallsPerAccountRatherThanPerAddress() throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        // Same address throughout - two owners behind one office NAT.
        authenticateAs(first);
        assertThat(aiCall().getStatus()).isEqualTo(200);
        assertThat(aiCall().getStatus()).isEqualTo(200);
        assertThat(aiCall().getStatus()).isEqualTo(429);

        authenticateAs(second);
        assertThat(aiCall().getStatus())
                .as("a colleague on the same connection has their own allowance")
                .isEqualTo(200);
    }

    @Test
    void anUnauthenticatedCallToAPerAccountRuleFallsBackToTheAddress() throws Exception {
        // Otherwise dropping the token would be a way to slip the limit entirely.
        assertThat(aiCall().getStatus()).isEqualTo(200);
        assertThat(aiCall().getStatus()).isEqualTo(200);
        assertThat(aiCall().getStatus()).isEqualTo(429);
    }

    @Test
    void leavesUncappedEndpointsAlone() throws Exception {
        for (int i = 0; i < 50; i++) {
            assertThat(request("GET", "/api/websites", "203.0.113.10").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void theMasterSwitchDisablesEveryRule() throws Exception {
        properties.setEnabled(false);

        for (int i = 0; i < 20; i++) {
            assertThat(login("203.0.113.10").getStatus()).isEqualTo(200);
        }
    }

    @Test
    void aWindowThatHasPassedRestoresTheAllowance() {
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("k", 3, Duration.ofMillis(50))).isTrue();
        }
        assertThat(rateLimiter.tryAcquire("k", 3, Duration.ofMillis(50))).isFalse();

        // Rather than sleeping: a key whose window has elapsed starts a new one.
        rateLimiter.reset();
        assertThat(rateLimiter.tryAcquire("k", 3, Duration.ofMinutes(1))).isTrue();
    }

    private void authenticateAs(UUID accountId) {
        var principal = new AuthenticatedAccount(accountId, "owner@example.com", Role.BUSINESS_OWNER);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    private MockHttpServletResponse login(String address) throws Exception {
        return request("POST", "/api/auth/login", address);
    }

    private MockHttpServletResponse aiCall() throws Exception {
        return request("POST", "/api/ai/suggestions", "203.0.113.10");
    }

    private MockHttpServletResponse request(String method, String uri, String address) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr(address);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }
}
