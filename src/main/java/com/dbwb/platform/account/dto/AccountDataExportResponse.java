package com.dbwb.platform.account.dto;

import com.dbwb.platform.delivery.dto.DeliveryAreaResponse;
import com.dbwb.platform.menu.dto.CategoryDto;
import com.dbwb.platform.menu.dto.MenuItemResponse;
import com.dbwb.platform.profile.dto.BusinessProfileResponse;
import com.dbwb.platform.profile.dto.OpeningHoursEntry;
import com.dbwb.platform.website.dto.WebsiteResponse;

import java.time.Instant;
import java.util.List;

/**
 * BR-DATA-005: a complete copy of the Owner's business data, offered before
 * permanent account deletion. TBD-013 leaves the exact file format open -
 * this is exposed as JSON, the most broadly-usable structured format,
 * pending a specific format decision (e.g. also offering a ZIP of CSVs).
 */
public record AccountDataExportResponse(
        String accountEmail,
        Instant exportedAt,
        List<WebsiteExport> websites
) {
    public record WebsiteExport(
            WebsiteResponse website,
            BusinessProfileResponse profile,
            List<OpeningHoursEntry> openingHours,
            List<CategoryDto> categories,
            List<MenuItemResponse> menuItems,
            List<DeliveryAreaResponse> deliveryAreas
    ) {
    }
}
