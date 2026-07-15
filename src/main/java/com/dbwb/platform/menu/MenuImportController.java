package com.dbwb.platform.menu;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.menu.dto.ConfirmImportRequest;
import com.dbwb.platform.menu.dto.ImportOutcomeResponse;
import com.dbwb.platform.menu.dto.ImportPreviewResponse;
import com.dbwb.platform.security.CurrentAccount;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/** BRD 9.8: Excel/CSV menu import - see MenuImportService for why only CSV is implemented so far. */
@RestController
@RequestMapping("/api/websites/{websiteId}/menu/import")
public class MenuImportController {

    private final MenuImportService menuImportService;
    private final CurrentAccount currentAccount;

    public MenuImportController(MenuImportService menuImportService, CurrentAccount currentAccount) {
        this.menuImportService = menuImportService;
        this.currentAccount = currentAccount;
    }

    @PostMapping("/preview")
    public ApiResponse<ImportPreviewResponse> preview(@PathVariable UUID websiteId, @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(menuImportService.preview(websiteId, currentAccount.get(), file));
    }

    @PostMapping("/confirm")
    public ApiResponse<ImportOutcomeResponse> confirm(@PathVariable UUID websiteId,
                                                        @RequestPart("file") MultipartFile file,
                                                        @RequestPart("decisions") ConfirmImportRequest request) {
        var outcome = menuImportService.confirm(websiteId, currentAccount.get(), file, request);
        return ApiResponse.ok(outcome, "Import complete.");
    }
}
