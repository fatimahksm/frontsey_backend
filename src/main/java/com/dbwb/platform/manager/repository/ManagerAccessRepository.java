package com.dbwb.platform.manager.repository;

import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagerAccessRepository extends JpaRepository<ManagerAccess, UUID> {
    List<ManagerAccess> findByWebsiteId(UUID websiteId);

    /** Fetch-joins the website: every caller of this immediately needs website fields (business name, slug, ...) once outside this transaction, e.g. in a controller's DTO mapping. */
    @Query("SELECT a FROM ManagerAccess a JOIN FETCH a.website WHERE a.managerAccount.id = :managerAccountId AND a.status = :status")
    List<ManagerAccess> findByManagerAccountIdAndStatus(@Param("managerAccountId") UUID managerAccountId, @Param("status") InvitationStatus status);
    Optional<ManagerAccess> findByWebsiteIdAndManagerAccountId(UUID websiteId, UUID managerAccountId);
    long countByWebsiteIdAndStatusIn(UUID websiteId, Collection<InvitationStatus> statuses);

    /** BR-MGR-002: pending invitations sent before the invitee had an account, keyed by the email they were invited at. */
    List<ManagerAccess> findByInvitedEmailIgnoreCaseAndStatus(String invitedEmail, InvitationStatus status);

    /** BR-MGR-001: an active (pending or accepted) invite/access record already exists for this email on this website. */
    boolean existsByWebsiteIdAndInvitedEmailIgnoreCaseAndStatusIn(UUID websiteId, String invitedEmail, Collection<InvitationStatus> statuses);

    /** BR-MGR-007: PENDING invitations old enough to auto-expire. */
    List<ManagerAccess> findByStatusAndCreatedAtBefore(InvitationStatus status, Instant cutoff);
}
