package com.dbwb.platform.auth;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import com.dbwb.platform.auth.dto.AuthResponse;
import com.dbwb.platform.auth.dto.LoginRequest;
import com.dbwb.platform.auth.dto.RegisterRequest;
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.config.FrontendProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.manager.ManagerService;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.security.JwtService;
import com.dbwb.platform.testsupport.TestEntities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountTokenRepository tokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private BusinessRuleProperties businessRules;
    @Mock
    private ManagerService managerService;
    @Mock
    private FrontendProperties frontendProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(frontendProperties.getPublicSiteBaseUrl()).thenReturn("http://localhost:3000");
        lenient().when(businessRules.getEmailVerificationTokenTtlHours()).thenReturn(24);
        authService = new AuthService(
                accountRepository, tokenRepository, passwordEncoder, jwtService, emailService, businessRules, managerService, frontendProperties);
    }

    @Test
    void registerRejectsAnAlreadyRegisteredEmail() {
        when(accountRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("owner@example.com", "password123", "Owner")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void registerLinksAnyPendingManagerInvitationsForTheNewAccount() {
        when(accountRepository.existsByEmailIgnoreCase("manager@example.com")).thenReturn(false);

        authService.register(new RegisterRequest("manager@example.com", "password123", "Manager"));

        // This is the exact bug this session found: linkPendingInvitationsForNewAccount
        // existed but register() never called it, so an invited Manager could never
        // actually get access once they signed up.
        verify(managerService).linkPendingInvitationsForNewAccount(any(Account.class));
    }

    @Test
    void loginRejectsWrongPassword() {
        Account account = activeAccount();
        when(accountRepository.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", account.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(account.getEmail(), "wrong")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void loginRejectsAnUnverifiedAccount() {
        Account account = activeAccount();
        account.setEmailVerified(false);
        when(accountRepository.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", account.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(account.getEmail(), "password123")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("verify your email");
    }

    @Test
    void loginRejectsADeletedAccount() {
        Account account = activeAccount();
        account.setStatus(AccountStatus.DELETED);
        when(accountRepository.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", account.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest(account.getEmail(), "password123")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void loginAllowsADisabledPendingDeletionAccountSoItCanBeRecovered() {
        // BR-AUTH-006: this is the only way an Owner can ever reach
        // AccountService.cancelDeletion() - login must not block this status.
        Account account = activeAccount();
        account.setStatus(AccountStatus.DISABLED_PENDING_DELETION);
        when(accountRepository.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", account.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(account)).thenReturn("a-jwt-token");

        AuthResponse response = authService.login(new LoginRequest(account.getEmail(), "password123"));

        assertThat(response.accessToken()).isEqualTo("a-jwt-token");
    }

    @Test
    void loginSucceedsForAnActiveVerifiedAccountWithTheRightPassword() {
        Account account = activeAccount();
        when(accountRepository.findByEmailIgnoreCase(account.getEmail())).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", account.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(account)).thenReturn("a-jwt-token");

        AuthResponse response = authService.login(new LoginRequest(account.getEmail(), "password123"));

        assertThat(response.accessToken()).isEqualTo("a-jwt-token");
        assertThat(response.email()).isEqualTo(account.getEmail());
        assertThat(response.role()).isEqualTo(Role.BUSINESS_OWNER);
    }

    private Account activeAccount() {
        Account account = new Account();
        account.setEmail("owner@example.com");
        account.setPasswordHash("hashed-password");
        account.setFullName("Owner");
        account.setRole(Role.BUSINESS_OWNER);
        account.setStatus(AccountStatus.ACTIVE);
        account.setEmailVerified(true);
        return TestEntities.withId(account, UUID.randomUUID());
    }
}
