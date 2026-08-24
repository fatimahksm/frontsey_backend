package com.dbwb.platform.security;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.common.config.JwtProperties;
import com.dbwb.platform.testsupport.TestEntities;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The filter used to take the caller's role straight from the token's claims,
 * which are a snapshot up to the access token's full 60-minute lifetime old.
 *
 * That made two states unreachable in time: a Super Admin who had just been
 * demoted kept SUPER_ADMIN - which short-circuits every check in
 * WebsiteAccessGuard - and a deleted account kept working. Revoking the
 * refresh token does not close either window; it only stops the next renewal,
 * not the access token already issued.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;
    private JwtService jwtService;

    private final UUID accountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-only-secret-not-for-production-use-0123456789");
        properties.setAccessTokenTtlMinutes(60);
        jwtService = new JwtService(properties);
        filter = new JwtAuthFilter(jwtService, accountRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usesTheAccountsCurrentRoleRatherThanTheOneInTheToken() throws Exception {
        // Token minted while they were a Super Admin.
        String token = tokenFor(Role.SUPER_ADMIN);
        // Since demoted, with that token still in hand and still unexpired.
        stubAuthorization(Role.BUSINESS_OWNER, AccountStatus.ACTIVE);

        AuthenticatedAccount principal = authenticateWith(token);

        assertThat(principal).isNotNull();
        assertThat(principal.role()).isEqualTo(Role.BUSINESS_OWNER);
    }

    @Test
    void refusesAnAccountThatHasSinceBeenDeleted() throws Exception {
        String token = tokenFor(Role.BUSINESS_OWNER);
        stubAuthorization(Role.BUSINESS_OWNER, AccountStatus.DELETED);

        assertThat(authenticateWith(token)).isNull();
    }

    @Test
    void refusesATokenWhoseAccountNoLongerExists() throws Exception {
        String token = tokenFor(Role.BUSINESS_OWNER);
        when(accountRepository.findAuthorizationById(accountId)).thenReturn(Optional.empty());

        assertThat(authenticateWith(token)).isNull();
    }

    @Test
    void stillAdmitsAnAccountDisabledPendingDeletion() throws Exception {
        // BR-AUTH-006: signing in is how the owner reaches cancelDeletion()
        // inside the retention window, so this one must keep working.
        String token = tokenFor(Role.BUSINESS_OWNER);
        stubAuthorization(Role.BUSINESS_OWNER, AccountStatus.DISABLED_PENDING_DELETION);

        assertThat(authenticateWith(token)).isNotNull();
    }

    @Test
    void leavesARequestWithNoTokenUnauthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private AuthenticatedAccount authenticateWith(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : (AuthenticatedAccount) authentication.getPrincipal();
    }

    private void stubAuthorization(Role role, AccountStatus status) {
        when(accountRepository.findAuthorizationById(accountId)).thenReturn(Optional.of(
                new AccountRepository.AccountAuthorization() {
                    @Override
                    public Role getRole() {
                        return role;
                    }

                    @Override
                    public AccountStatus getStatus() {
                        return status;
                    }
                }));
    }

    private String tokenFor(Role role) {
        Account account = TestEntities.withId(new Account(), accountId);
        account.setEmail("someone@example.com");
        account.setRole(role);
        return jwtService.generateAccessToken(account);
    }
}
