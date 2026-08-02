package com.dbwb.platform.auth;

import com.dbwb.platform.account.entity.TokenType;
import com.dbwb.platform.account.repository.AccountTokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * BR-AUTH-007: revokes every other still-active refresh token for an
 * account when a reused/replayed token indicates possible theft. This runs
 * in its own REQUIRES_NEW transaction, deliberately separate from
 * AuthService.refresh() - that method throws afterward to reject the
 * request, which would otherwise roll back the revocation too (Spring's
 * default behavior on an unchecked exception) since a self-invoked
 * @Transactional method on the same bean doesn't go through the proxy.
 */
@Component
public class RefreshTokenRevoker {

    private final AccountTokenRepository tokenRepository;

    public RefreshTokenRevoker(AccountTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revokeAllActive(UUID accountId, TokenType type) {
        tokenRepository.findActiveByAccountIdAndType(accountId, type)
                .forEach(t -> t.setUsedAt(Instant.now()));
    }
}
