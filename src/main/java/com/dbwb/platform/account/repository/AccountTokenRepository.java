package com.dbwb.platform.account.repository;

import com.dbwb.platform.account.entity.AccountToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountTokenRepository extends JpaRepository<AccountToken, UUID> {
    Optional<AccountToken> findByToken(String token);
}
