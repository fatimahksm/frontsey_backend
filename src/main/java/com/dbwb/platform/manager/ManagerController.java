package com.dbwb.platform.manager;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.manager.dto.InviteManagerRequest;
import com.dbwb.platform.manager.dto.ManagerAccessResponse;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/managers")
public class ManagerController {

    private final ManagerService managerService;
    private final CurrentAccount currentAccount;

    public ManagerController(ManagerService managerService, CurrentAccount currentAccount) {
        this.managerService = managerService;
        this.currentAccount = currentAccount;
    }

    @PostMapping
    public ApiResponse<ManagerAccessResponse> invite(@PathVariable UUID websiteId, @Valid @RequestBody InviteManagerRequest request) {
        var access = managerService.invite(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(ManagerAccessResponse.from(access), "Invitation sent.");
    }

    @GetMapping
    public ApiResponse<List<ManagerAccessResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(managerService.listForWebsite(websiteId, currentAccount.get())
                .stream().map(ManagerAccessResponse::from).toList());
    }

    @PutMapping("/{accessId}/permissions")
    public ApiResponse<Void> updatePermissions(@PathVariable UUID websiteId, @PathVariable UUID accessId,
                                                @RequestBody Set<Permission> permissions) {
        managerService.updatePermissions(websiteId, accessId, currentAccount.get(), permissions);
        return ApiResponse.ok(null, "Permissions updated.");
    }

    @DeleteMapping("/{accessId}")
    public ApiResponse<Void> revoke(@PathVariable UUID websiteId, @PathVariable UUID accessId) {
        managerService.revoke(websiteId, accessId, currentAccount.get());
        return ApiResponse.ok(null, "Manager access revoked.");
    }
}
