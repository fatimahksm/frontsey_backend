package com.dbwb.platform.profile;

import com.dbwb.platform.audit.AuditService;
import com.dbwb.platform.manager.entity.Permission;
import com.dbwb.platform.profile.dto.BusinessProfileRequest;
import com.dbwb.platform.profile.dto.OpeningHoursEntry;
import com.dbwb.platform.profile.entity.BusinessProfile;
import com.dbwb.platform.profile.entity.OpeningHours;
import com.dbwb.platform.profile.repository.BusinessProfileRepository;
import com.dbwb.platform.profile.repository.OpeningHoursRepository;
import com.dbwb.platform.security.AuthenticatedAccount;
import com.dbwb.platform.website.WebsiteAccessGuard;
import com.dbwb.platform.website.entity.BusinessWebsite;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/** BRD 9.4/9.5: owner/permitted-Manager managed public profile fields and weekly opening hours. */
@Service
public class ProfileService {

    private final BusinessProfileRepository profileRepository;
    private final OpeningHoursRepository openingHoursRepository;
    private final WebsiteAccessGuard accessGuard;
    private final AuditService auditService;

    public ProfileService(
            BusinessProfileRepository profileRepository,
            OpeningHoursRepository openingHoursRepository,
            WebsiteAccessGuard accessGuard,
            AuditService auditService) {
        this.profileRepository = profileRepository;
        this.openingHoursRepository = openingHoursRepository;
        this.accessGuard = accessGuard;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Optional<BusinessProfile> getProfile(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        return profileRepository.findByWebsiteId(websiteId);
    }

    @Transactional
    public BusinessProfile updateProfile(UUID websiteId, AuthenticatedAccount caller, BusinessProfileRequest request) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_BUSINESS_PROFILE);
        BusinessProfile profile = profileRepository.findByWebsiteId(websiteId).orElseGet(() -> {
            BusinessProfile created = new BusinessProfile();
            created.setWebsite(website);
            return created;
        });

        profile.setDescription(request.description());
        profile.setLogoUrl(request.logoUrl());
        profile.setCoverImageUrl(request.coverImageUrl());
        profile.setPhone(request.phone());
        profile.setWhatsappNumber(request.whatsappNumber());
        profile.setEmail(request.email());
        profile.setAddress(request.address());
        profile.setGoogleMapsUrl(request.googleMapsUrl());
        profile.setInstagramUrl(request.instagramUrl());
        profile.setTiktokUrl(request.tiktokUrl());
        profile.setShowPrivacyPolicy(request.showPrivacyPolicy());
        profile.setPrivacyPolicyContent(request.privacyPolicyContent());
        profile.setShowTermsAndConditions(request.showTermsAndConditions());
        profile.setTermsAndConditionsContent(request.termsAndConditionsContent());
        profile.setShowDeliveryPolicy(request.showDeliveryPolicy());
        profile.setDeliveryPolicyContent(request.deliveryPolicyContent());
        profile.setShowRefundPolicy(request.showRefundPolicy());
        profile.setRefundPolicyContent(request.refundPolicyContent());

        profileRepository.save(profile);
        auditService.record(caller.accountId(), "BUSINESS_PROFILE_UPDATED", websiteId.toString());
        return profile;
    }

    /** Always returns exactly 7 entries (Sunday..Saturday), defaulting unset days to closed. */
    @Transactional(readOnly = true)
    public List<OpeningHoursEntry> getOpeningHours(UUID websiteId, AuthenticatedAccount caller) {
        accessGuard.requireReadAccess(websiteId, caller);
        Map<DayOfWeek, OpeningHours> byDay = openingHoursRepository.findByWebsiteIdOrderByDayOfWeek(websiteId)
                .stream().collect(Collectors.toMap(OpeningHours::getDayOfWeek, h -> h));

        return List.of(DayOfWeek.values()).stream()
                .map(day -> byDay.containsKey(day)
                        ? OpeningHoursEntry.from(byDay.get(day))
                        : new OpeningHoursEntry(day, false, null, null))
                .toList();
    }

    /** BR-HRS-001/002: replaces the full week in one call - partial updates would leave stale days behind. */
    @Transactional
    public List<OpeningHoursEntry> updateOpeningHours(UUID websiteId, AuthenticatedAccount caller, List<OpeningHoursEntry> entries) {
        BusinessWebsite website = accessGuard.requirePermission(websiteId, caller, Permission.MANAGE_BUSINESS_PROFILE);

        Map<DayOfWeek, OpeningHours> existingByDay = openingHoursRepository.findByWebsiteIdOrderByDayOfWeek(websiteId)
                .stream().collect(Collectors.toMap(OpeningHours::getDayOfWeek, h -> h));

        for (OpeningHoursEntry entry : entries) {
            OpeningHours hours = existingByDay.computeIfAbsent(entry.dayOfWeek(), day -> {
                OpeningHours created = new OpeningHours();
                created.setWebsite(website);
                created.setDayOfWeek(day);
                return created;
            });
            hours.setOpen(entry.open());
            hours.setOpensAt(entry.open() ? entry.opensAt() : null);
            hours.setClosesAt(entry.open() ? entry.closesAt() : null);
            openingHoursRepository.save(hours);
        }

        auditService.record(caller.accountId(), "OPENING_HOURS_UPDATED", websiteId.toString());
        return getOpeningHours(websiteId, caller);
    }
}
