package com.dbwb.platform.profile;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.profile.dto.BusinessProfileRequest;
import com.dbwb.platform.profile.dto.BusinessProfileResponse;
import com.dbwb.platform.profile.dto.OpeningHoursEntry;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}")
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentAccount currentAccount;

    public ProfileController(ProfileService profileService, CurrentAccount currentAccount) {
        this.profileService = profileService;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/profile")
    public ApiResponse<BusinessProfileResponse> getProfile(@PathVariable UUID websiteId) {
        return ApiResponse.ok(profileService.getProfile(websiteId, currentAccount.get())
                .map(BusinessProfileResponse::from)
                .orElseGet(BusinessProfileResponse::empty));
    }

    @PutMapping("/profile")
    public ApiResponse<BusinessProfileResponse> updateProfile(@PathVariable UUID websiteId,
                                                                @Valid @RequestBody BusinessProfileRequest request) {
        var profile = profileService.updateProfile(websiteId, currentAccount.get(), request);
        return ApiResponse.ok(BusinessProfileResponse.from(profile), "Profile updated.");
    }

    @GetMapping("/opening-hours")
    public ApiResponse<List<OpeningHoursEntry>> getOpeningHours(@PathVariable UUID websiteId) {
        return ApiResponse.ok(profileService.getOpeningHours(websiteId, currentAccount.get()));
    }

    @PutMapping("/opening-hours")
    public ApiResponse<List<OpeningHoursEntry>> updateOpeningHours(@PathVariable UUID websiteId,
                                                                     @Valid @RequestBody List<OpeningHoursEntry> entries) {
        var updated = profileService.updateOpeningHours(websiteId, currentAccount.get(), entries);
        return ApiResponse.ok(updated, "Opening hours updated.");
    }
}
