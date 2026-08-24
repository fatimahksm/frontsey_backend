package com.dbwb.platform.account.repository;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.AccountStatus;
import com.dbwb.platform.account.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    /** BR-AUTH-006: accounts whose disable window has elapsed and are due for permanent deletion. */
    List<Account> findByStatusAndDisabledAtBefore(AccountStatus status, Instant instant);

    /**
     * The role and status as they are right now, for JwtAuthFilter to check on
     * every authenticated request. A projection rather than the entity: this
     * runs on every call, and the filter needs two columns, not the whole row
     * plus its associations.
     */
    @Query("select a.role as role, a.status as status from Account a where a.id = :id")
    Optional<AccountAuthorization> findAuthorizationById(@Param("id") UUID id);

    /** Just the two fields JwtAuthFilter re-reads per request. */
    interface AccountAuthorization {
        Role getRole();

        AccountStatus getStatus();
    }
}
