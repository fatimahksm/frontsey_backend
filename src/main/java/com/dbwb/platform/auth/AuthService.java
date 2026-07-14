package com.dbwb.platform.auth;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.AccountToken;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.account.entity.TokenType;
import com.dbwb.platform.account.repository.AccountRepository;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import com.dbwb.platform.auth.dto.AuthResponse;
import com.dbwb.platform.auth.dto.LoginRequest;
import com.dbwb.platform.auth.dto.RegisterRequest;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Implements BR-AUTH-001..005: registration, mandatory email verification
 * before login, email+password login only (no social/phone login in MVP),
 * and password reset.
 */
@Service
public class AuthService {

    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;
    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final AccountRepository accountRepository;
    private final AccountTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final String publicSiteBaseUrl;

    public AuthService(
            AccountRepository accountRepository,
            AccountTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            @Value("${dbwb.frontend.public-site-base-url}") String publicSiteBaseUrl) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.publicSiteBaseUrl = publicSiteBaseUrl;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (accountRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleViolationException("An account with this email already exists.");
        }

        Account account = new Account();
        account.setEmail(request.email());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setFullName(request.fullName());
        account.setRole(Role.BUSINESS_OWNER); // self-registration is always as a Business Owner
        account.setStatus(AccountStatus.PENDING_VERIFICATION);
        accountRepository.save(account);

        issueVerificationEmail(account);
    }

    private void issueVerificationEmail(Account account) {
        AccountToken token = new AccountToken();
        token.setAccount(account);
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(VERIFICATION_TOKEN_TTL_HOURS, ChronoUnit.HOURS));
        tokenRepository.save(token);

        String verifyLink = publicSiteBaseUrl + "/verify-email?token=" + token.getToken();
        emailService.send(account.getEmail(), "Verify your email",
                "Welcome! Please verify your email address: " + verifyLink);
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        AccountToken token = tokenRepository.findByToken(rawToken)
                .filter(t -> t.getType() == TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token."));

        if (!token.isValid()) {
            throw new BusinessRuleViolationException("This verification link has expired.");
        }

        Account account = token.getAccount();
        account.setEmailVerified(true);
        account.setStatus(AccountStatus.ACTIVE);
        token.setUsedAt(Instant.now());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessRuleViolationException("Invalid email or password.");
        }

        // BR-AUTH-002: unverified accounts cannot access the platform.
        if (!account.isEmailVerified()) {
            throw new BusinessRuleViolationException("Please verify your email before logging in.");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException("This account is not active.");
        }

        String accessToken = jwtService.generateAccessToken(account);
        return new AuthResponse(accessToken, account.getId(), account.getEmail(), account.getRole());
    }

    @Transactional
    public void requestPasswordReset(String email) {
        // Always respond as if successful regardless of whether the email exists,
        // to avoid leaking which emails are registered.
        accountRepository.findByEmailIgnoreCase(email).ifPresent(account -> {
            AccountToken token = new AccountToken();
            token.setAccount(account);
            token.setType(TokenType.PASSWORD_RESET);
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES));
            tokenRepository.save(token);

            String resetLink = publicSiteBaseUrl + "/reset-password?token=" + token.getToken();
            emailService.send(account.getEmail(), "Reset your password",
                    "Use this time-limited link to reset your password: " + resetLink);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        AccountToken token = tokenRepository.findByToken(rawToken)
                .filter(t -> t.getType() == TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token."));

        if (!token.isValid()) {
            throw new BusinessRuleViolationException("This reset link has expired.");
        }

        Account account = token.getAccount();
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        token.setUsedAt(Instant.now());
    }
}
