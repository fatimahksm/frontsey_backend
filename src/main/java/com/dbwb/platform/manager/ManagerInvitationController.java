package com.dbwb.platform.manager;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.manager.dto.ManagerInvitationResponse;
import com.dbwb.platform.security.CurrentAccount;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Phase 4 (BR-MGR-008): the invited person's own view of their manager
 * invitations - distinct from ManagerController, which is the website
 * Owner's view (invite/list/revoke for one website). Not website-scoped
 * since the invitee doesn't have access to the website yet.
 */
@RestController
@RequestMapping("/api/managers/invitations")
public class ManagerInvitationController {

    private final ManagerService managerService;
    private final CurrentAccount currentAccount;

    public ManagerInvitationController(ManagerService managerService, CurrentAccount currentAccount) {
        this.managerService = managerService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<ManagerInvitationResponse>> list() {
        var invitations = managerService.listMyInvitations(currentAccount.get()).stream()
                .map(ManagerInvitationResponse::from).toList();
        return ApiResponse.ok(invitations);
    }

    @PostMapping("/{accessId}/accept")
    public ApiResponse<ManagerInvitationResponse> accept(@PathVariable UUID accessId) {
        var access = managerService.accept(accessId, currentAccount.get());
        return ApiResponse.ok(ManagerInvitationResponse.from(access), "Invitation accepted.");
    }

    @PostMapping("/{accessId}/reject")
    public ApiResponse<Void> reject(@PathVariable UUID accessId) {
        managerService.reject(accessId, currentAccount.get());
        return ApiResponse.ok(null, "Invitation declined.");
    }
}
