package com.dbwb.platform.security;

import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Mirrors AuthService.login. DISABLED_PENDING_DELETION is deliberately
     * allowed: BR-AUTH-006 makes signing in how an owner reaches
     * AccountService.cancelDeletion() to recover the account inside the
     * retention window, so cutting it off here would strand them.
     */
    private static final Set<AccountStatus> USABLE_STATUSES =
            EnumSet.of(AccountStatus.ACTIVE, AccountStatus.DISABLED_PENDING_DELETION);

    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    public JwtAuthFilter(JwtService jwtService, AccountRepository accountRepository) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
    }

    /**
     * Authenticates the request from its bearer token, but takes the caller's
     * role and status from the database rather than from the token's claims.
     *
     * The claims are a 60-minute-old snapshot. Trusting them meant a Super
     * Admin who had just been demoted kept SUPER_ADMIN - which bypasses every
     * check in WebsiteAccessGuard - and a suspended or deleted account kept
     * working, both for the rest of the token's life. Revoking the refresh
     * token does not help: it only stops the next renewal, not the access
     * token already in hand.
     *
     * The cost is one indexed lookup of two columns per authenticated request.
     * A token whose account has since been deleted, or whose id no longer
     * resolves, simply leaves the context unauthenticated - the entry point in
     * SecurityConfig then answers 401, and the client's silent refresh will
     * fail too, ending the session.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isValid(token)) {
                Claims claims = jwtService.parseClaims(token);
                UUID accountId = UUID.fromString(claims.getSubject());

                accountRepository.findAuthorizationById(accountId)
                        .filter(account -> USABLE_STATUSES.contains(account.getStatus()))
                        .ifPresent(account -> authenticate(accountId, claims.get("email", String.class), account.getRole()));
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(UUID accountId, String email, Role role) {
        AuthenticatedAccount principal = new AuthenticatedAccount(accountId, email, role);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
