package com.dbwb.platform.security;

import com.dbwb.platform.common.config.RateLimitProperties;
import com.dbwb.platform.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Caps how often the endpoints worth abusing can be called.
 *
 * Nothing capped them before, which left three separate holes: login,
 * registration and password-reset were unbounded, so credential stuffing was
 * limited only by bandwidth; the public item-view endpoint is anonymous and
 * writes a row per call, so anyone could inflate a site's analytics and grow
 * the table without limit; and the AI endpoint, while authenticated, spends
 * real money at OpenRouter on every call, so any single account could run up
 * the platform's bill.
 *
 * Runs after JwtAuthFilter so an authenticated rule can key on the account
 * rather than the IP - several owners behind one office NAT must not share an
 * AI allowance.
 *
 * Answers 429 in the standard envelope. The frontend already models that
 * status as a distinct, retryable failure kind (see lib/api/errors.ts), so it
 * surfaces as its own message rather than a generic error.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** One capped endpoint: how to recognise it, what it costs, and what counts as "the same caller". */
    private record Rule(String method, String pathPrefix, String pathContains,
                        Function<RateLimitProperties, RateLimitProperties.Policy> policy,
                        boolean perAccount, String name) {

        static Rule of(String method, String pathPrefix,
                       Function<RateLimitProperties, RateLimitProperties.Policy> policy,
                       boolean perAccount, String name) {
            return new Rule(method, pathPrefix, null, policy, perAccount, name);
        }

        boolean matches(HttpServletRequest request) {
            String uri = request.getRequestURI();
            return method.equals(request.getMethod())
                    && uri.startsWith(pathPrefix)
                    && (pathContains == null || uri.contains(pathContains));
        }
    }

    /**
     * First match wins, and the two public POSTs are ordered accordingly.
     *
     * Counting a visit is a POST now rather than a side effect of the GET (see
     * PublicWebsiteController), and both beacons sit under the same prefix and
     * both end in "/view" - so the item one is matched on the "/items/" in its
     * own path and listed first, and the page one takes what is left. The page
     * beacon has to keep the page-view allowance, four times the item one: a
     * shared address - an office, a cafe, a carrier's NAT - is many people
     * reading one site.
     */
    private static final List<Rule> RULES = List.of(
            Rule.of("POST", "/api/auth/login", RateLimitProperties::getLogin, false, "login"),
            Rule.of("POST", "/api/auth/register", RateLimitProperties::getRegistration, false, "registration"),
            Rule.of("POST", "/api/auth/password-reset/request", RateLimitProperties::getPasswordReset, false, "password-reset"),
            Rule.of("POST", "/api/ai/", RateLimitProperties::getAiSuggestions, true, "ai"),
            Rule.of("POST", "/api/uploads/", RateLimitProperties::getUploads, true, "uploads"),
            new Rule("POST", "/api/public/websites/", "/items/", RateLimitProperties::getPublicItemView, false, "item-view"),
            Rule.of("POST", "/api/public/websites/", RateLimitProperties::getPublicPageView, false, "page-view"),
            Rule.of("GET", "/api/public/websites/", RateLimitProperties::getPublicPageView, false, "page-view"));

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimitProperties properties, RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        for (Rule rule : RULES) {
            if (!rule.matches(request)) {
                continue;
            }
            RateLimitProperties.Policy policy = rule.policy().apply(properties);
            String key = rule.name() + ":" + callerKey(request, rule.perAccount());

            if (!rateLimiter.tryAcquire(key, policy.getLimit(), Duration.ofMinutes(policy.getWindowMinutes()))) {
                reject(response, policy.getWindowMinutes());
                return;
            }
            break;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * An authenticated rule keys on the account; everything else on the client
     * address. An unauthenticated call to a per-account rule falls back to the
     * address, so it cannot slip the limit by simply omitting a token.
     */
    private String callerKey(HttpServletRequest request, boolean perAccount) {
        if (perAccount) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedAccount account) {
                return "account:" + account.accountId();
            }
        }
        return "ip:" + request.getRemoteAddr();
    }

    /**
     * Deliberately reads getRemoteAddr() and not X-Forwarded-For: that header
     * is caller-supplied, so trusting it directly would let anyone reset their
     * own counter per request by inventing an address. A deployment behind a
     * proxy should set server.forward-headers-strategy=framework, which makes
     * Spring resolve the real client address into getRemoteAddr() from headers
     * it is configured to trust.
     */
    private void reject(HttpServletResponse response, int windowMinutes) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(windowMinutes * 60));
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(
                "Too many attempts. Please wait a few minutes and try again.")));
    }
}
