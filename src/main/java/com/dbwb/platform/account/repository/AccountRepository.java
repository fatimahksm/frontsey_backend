package com.dbwb.platform.account.repository;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    /** BR-AUTH-006: accounts whose disable window has elapsed and are due for permanent deletion. */
    List<Account> findByStatusAndDisabledAtBefore(AccountStatus status, Instant instant);
}
