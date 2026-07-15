package com.dbwb.platform.website;

import com.dbwb.platform.account.entity.Account;
import com.dbwb.platform.account.entity.Role;
import com.dbwb.platform.common.exception.AccessDeniedForTenantException;
import com.dbwb.platform.common.exception.ResourceNotFoundException;
import com.dbwb.platform.manager.entity.InvitationStatus;
import com.dbwb.platform.manager.entity.ManagerAccess;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.manager.repository.ManagerAccessRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.testsupport.TestEntities;
import com.dbwb.platform.website.entity.BusinessWebsite;
import com.dbwb.platform.website.repository.BusinessWebsiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * BR-RULE-003: this guard is the single tenant-isolation choke point every
 * module routes through, so its correctness matters more than almost
 * anything else in the codebase - a bug here is a bug in every module at once.
 */
@ExtendWith(MockitoExtension.class)
class WebsiteAccessGuardTest {

    @Mock
    private BusinessWebsiteRepository websiteRepository;
    @Mock
    private ManagerAccessRepository managerAccessRepository;

    private WebsiteAccessGuard guard;

    private final UUID websiteId = UUID.randomUUID();
    private BusinessWebsite website;
    private Account owner;

    @BeforeEach
    void setUp() {
        guard = new WebsiteAccessGuard(websiteRepository, managerAccessRepository);
        owner = TestEntities.withId(new Account(), UUID.randomUUID());
        website = TestEntities.withId(new BusinessWebsite(), websiteId);
        website.setOwner(owner);
        lenient().when(websiteRepository.findById(websiteId)).thenReturn(Optional.of(website));
    }

    @Test
    void unknownWebsiteRaisesNotFoundRegardlessOfCaller() {
        when(websiteRepository.findById(websiteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireReadAccess(websiteId, ownerCaller()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ownerAlwaysHasReadAccess() {
        assertThat(guard.requireReadAccess(websiteId, ownerCaller())).isEqualTo(website);
    }

    @Test
    void superAdminAlwaysHasReadAccessEvenToSomeoneElsesWebsite() {
        AuthenticatedAccount superAdmin = new AuthenticatedAccount(UUID.randomUUID(), "admin@example.com", Role.SUPER_ADMIN);

        assertThat(guard.requireReadAccess(websiteId, superAdmin)).isEqualTo(website);
    }

    @Test
    void anAcceptedManagerHasReadAccess() {
        AuthenticatedAccount manager = managerCaller();
        when(managerAccessRepository.findByWebsiteIdAndManagerAccountId(websiteId, manager.accountId()))
                .thenReturn(Optional.of(acceptedAccessWithPermissions(Permission.VIEW_ANALYTICS)));

        assertThat(guard.requireReadAccess(websiteId, manager)).isEqualTo(website);
    }

    @Test
    void aPendingNotYetAcceptedManagerHasNoAccess() {
        AuthenticatedAccount manager = managerCaller();
        ManagerAccess pending = acceptedAccessWithPermissions(Permission.MANAGE_MENU);
        pending.setStatus(InvitationStatus.PENDING);
        when(managerAccessRepository.findByWebsiteIdAndManagerAccountId(websiteId, manager.accountId()))
                .thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> guard.requireReadAccess(websiteId, manager))
                .isInstanceOf(AccessDeniedForTenantException.class);
    }

    @Test
    void aStrangerWithNoRelationToTheWebsiteIsDenied() {
        AuthenticatedAccount stranger = new AuthenticatedAccount(UUID.randomUUID(), "stranger@example.com", Role.BUSINESS_OWNER);
        when(managerAccessRepository.findByWebsiteIdAndManagerAccountId(websiteId, stranger.accountId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> guard.requireReadAccess(websiteId, stranger))
                .isInstanceOf(AccessDeniedForTenantException.class);
    }

    @Test
    void ownerCanExerciseAnyPermissionWithoutBeingGrantedOne() {
        assertThat(guard.requirePermission(websiteId, ownerCaller(), Permission.MANAGE_MENU)).isEqualTo(website);
    }

    @Test
    void managerCanOnlyExerciseAnExplicitlyGrantedPermission() {
        AuthenticatedAccount manager = managerCaller();
        when(managerAccessRepository.findByWebsiteIdAndManagerAccountId(websiteId, manager.accountId()))
                .thenReturn(Optional.of(acceptedAccessWithPermissions(Permission.MANAGE_MENU)));

        assertThat(guard.requirePermission(websiteId, manager, Permission.MANAGE_MENU)).isEqualTo(website);
    }

    @Test
    void managerIsDeniedAPermissionTheyWerentGranted() {
        AuthenticatedAccount manager = managerCaller();
        when(managerAccessRepository.findByWebsiteIdAndManagerAccountId(websiteId, manager.accountId()))
                .thenReturn(Optional.of(acceptedAccessWithPermissions(Permission.MANAGE_MENU)));

        assertThatThrownBy(() -> guard.requirePermission(websiteId, manager, Permission.PUBLISH_WEBSITE))
                .isInstanceOf(AccessDeniedForTenantException.class);
    }

    @Test
    void onlyTheOwnerNotEvenAFullyPermittedManagerPassesRequireOwner() {
        AuthenticatedAccount manager = managerCaller();

        assertThatThrownBy(() -> guard.requireOwner(websiteId, manager))
                .isInstanceOf(AccessDeniedForTenantException.class);
        assertThat(guard.requireOwner(websiteId, ownerCaller())).isEqualTo(website);
    }

    private AuthenticatedAccount ownerCaller() {
        return new AuthenticatedAccount(owner.getId(), "owner@example.com", Role.BUSINESS_OWNER);
    }

    private AuthenticatedAccount managerCaller() {
        return new AuthenticatedAccount(UUID.randomUUID(), "manager@example.com", Role.MANAGER);
    }

    private ManagerAccess acceptedAccessWithPermissions(Permission... permissions) {
        ManagerAccess access = TestEntities.withId(new ManagerAccess(), UUID.randomUUID());
        access.setWebsite(website);
        access.setStatus(InvitationStatus.ACCEPTED);
        Set<Permission> set = EnumSet.noneOf(Permission.class);
        set.addAll(java.util.Arrays.asList(permissions));
        access.setPermissions(set);
        return access;
    }
}
