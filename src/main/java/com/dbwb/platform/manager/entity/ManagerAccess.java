package com.dbwb.platform.manager.entity;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.common.entity.BaseEntity;
import com.dbwb.platform.website.entity.BusinessWebsite;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/**
 * BR-MGR-001..006: one row per (Manager account, website) pair. Removing a
 * Manager (BR-MGR-005) deletes/revokes only this row - the underlying
 * Account is untouched, so the person can still log in and be re-invited
 * elsewhere.
 */
@Entity
@Table(name = "manager_access")
public class ManagerAccess extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "website_id", nullable = false)
    private BusinessWebsite website;

    /** Null until the invited email registers/accepts (BR-MGR-002). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_account_id")
    private Account managerAccount;

    @Column(nullable = false)
    private String invitedEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status = InvitationStatus.PENDING;

    @ElementCollection(targetClass = Permission.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "manager_access_permissions", joinColumns = @JoinColumn(name = "manager_access_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    private Set<Permission> permissions = new HashSet<>();

    // --- getters / setters ---

    public BusinessWebsite getWebsite() {
        return website;
    }

    public void setWebsite(BusinessWebsite website) {
        this.website = website;
    }

    public Account getManagerAccount() {
        return managerAccount;
    }

    public void setManagerAccount(Account managerAccount) {
        this.managerAccount = managerAccount;
    }

    public String getInvitedEmail() {
        return invitedEmail;
    }

    public void setInvitedEmail(String invitedEmail) {
        this.invitedEmail = invitedEmail;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
