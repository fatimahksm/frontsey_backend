package com.dbwb.platform.account.repository;

import com.dbwb.platform.account.entity.AccountToken;
import com.dbwb.platform.account.entity.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {
    Optional<AccountToken> findByToken(String token);

    /** BR-AUTH-007: every other still-unused refresh token for this account - revoked when a reused/stale refresh token indicates possible theft. */
    @Query("SELECT t FROM AccountToken t WHERE t.account.id = :accountId AND t.type = :type AND t.usedAt IS NULL")
    List<AccountToken> findActiveByAccountIdAndType(@Param("accountId") UUID accountId, @Param("type") TokenType type);
}
