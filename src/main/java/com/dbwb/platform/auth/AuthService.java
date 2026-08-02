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
import com.dbwb.platform.common.config.BusinessRuleProperties;
import com.dbwb.platform.common.config.FrontendProperties;
import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.ManagerService;
import com.dbwb.platform.notification.EmailService;
import com.dbwb.platform.security.JwtService;
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

    private final AccountRepository accountRepository;
    private final AccountTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final BusinessRuleProperties businessRules;
    private final ManagerService managerService;
    private final RefreshTokenRevoker refreshTokenRevoker;
    private final String publicSiteBaseUrl;

    public AuthService(
            AccountRepository accountRepository,
            AccountTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            BusinessRuleProperties businessRules,
            ManagerService managerService,
            RefreshTokenRevoker refreshTokenRevoker,
            FrontendProperties frontendProperties) {
        this.accountRepository = accountRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.businessRules = businessRules;
        this.managerService = managerService;
        this.refreshTokenRevoker = refreshTokenRevoker;
        this.publicSiteBaseUrl = frontendProperties.getPublicSiteBaseUrl();
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

        // BR-MGR-002: attach any Manager invitations sent to this email before the account existed.
        managerService.linkPendingInvitationsForNewAccount(account);

        issueVerificationEmail(account);
    }

    private void issueVerificationEmail(Account account) {
        AccountToken token = new AccountToken();
        token.setAccount(account);
        token.setType(TokenType.EMAIL_VERIFICATION);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(businessRules.getEmailVerificationTokenTtlHours(), ChronoUnit.HOURS));
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

    @Transactional
    public AuthResult login(LoginRequest request) {
        Account account = accountRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessRuleViolationException("Invalid email or password.");
        }

        // BR-AUTH-002: unverified accounts cannot access the platform.
        if (!account.isEmailVerified()) {
            throw new BusinessRuleViolationException("Please verify your email before logging in.");
        }

        // BR-AUTH-006: a disabled-pending-deletion account can still log in - that's how its
        // Owner reaches AccountService.cancelDeletion() to recover it within the retention window.
        if (account.getStatus() != AccountStatus.ACTIVE && account.getStatus() != AccountStatus.DISABLED_PENDING_DELETION) {
            throw new BusinessRuleViolationException("This account is not active.");
        }

        String accessToken = jwtService.generateAccessToken(account);
        AccountToken refreshToken = issueRefreshToken(account);
        return new AuthResult(
                new AuthResponse(accessToken, account.getId(), account.getEmail(), account.getRole()),
                refreshToken.getToken(),
                refreshToken.getExpiresAt());
    }

    /** BR-AUTH-007: rotates a refresh token - the presented one is consumed and a new access + refresh token pair is issued. Rejects (and revokes every other outstanding refresh token for the account) if the presented token was already used, since that indicates it was stolen and replayed. */
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        AccountToken token = tokenRepository.findByToken(rawRefreshToken)
                .filter(t -> t.getType() == TokenType.REFRESH)
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid session. Please log in again."));

        Account account = token.getAccount();

        if (token.getUsedAt() != null) {
            // Runs in its own committed transaction - see RefreshTokenRevoker's javadoc for why
            // this can't just be a repository loop here (this method's own rollback would undo it).
            refreshTokenRevoker.revokeAllActive(account.getId(), TokenType.REFRESH);
            throw new BusinessRuleViolationException("Invalid session. Please log in again.");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessRuleViolationException("Your session has expired. Please log in again.");
        }
        if (account.getStatus() != AccountStatus.ACTIVE && account.getStatus() != AccountStatus.DISABLED_PENDING_DELETION) {
            throw new BusinessRuleViolationException("This account is not active.");
        }

        token.setUsedAt(Instant.now());

        String accessToken = jwtService.generateAccessToken(account);
        AccountToken newRefreshToken = issueRefreshToken(account);
        return new AuthResult(
                new AuthResponse(accessToken, account.getId(), account.getEmail(), account.getRole()),
                newRefreshToken.getToken(),
                newRefreshToken.getExpiresAt());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        tokenRepository.findByToken(rawRefreshToken)
                .filter(t -> t.getType() == TokenType.REFRESH)
                .filter(t -> t.getUsedAt() == null)
                .ifPresent(t -> t.setUsedAt(Instant.now()));
    }

    private AccountToken issueRefreshToken(Account account) {
        AccountToken token = new AccountToken();
        token.setAccount(account);
        token.setType(TokenType.REFRESH);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plus(businessRules.getRefreshTokenTtlDays(), ChronoUnit.DAYS));
        return tokenRepository.save(token);
    }

    /** Bundles the JSON-safe AuthResponse with the raw refresh token/expiry so the controller can set it as an httpOnly cookie - the refresh token itself never appears in a JSON response body. */
    public record AuthResult(AuthResponse response, String refreshToken, Instant refreshTokenExpiresAt) {
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
            token.setExpiresAt(Instant.now().plus(businessRules.getPasswordResetTokenTtlMinutes(), ChronoUnit.MINUTES));
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
