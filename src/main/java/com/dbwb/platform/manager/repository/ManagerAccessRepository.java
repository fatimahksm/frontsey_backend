package com.dbwb.platform.manager.repository;

import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManagerAccessRepository extends JpaRepository<ManagerAccess, UUID> {
    List<ManagerAccess> findByWebsiteId(UUID websiteId);
    List<ManagerAccess> findByManagerAccountIdAndStatus(UUID managerAccountId, InvitationStatus status);
    Optional<ManagerAccess> findByWebsiteIdAndManagerAccountId(UUID websiteId, UUID managerAccountId);
    long countByWebsiteIdAndStatus(UUID websiteId, InvitationStatus status);
}
