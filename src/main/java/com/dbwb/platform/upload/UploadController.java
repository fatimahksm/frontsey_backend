package com.dbwb.platform.upload;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.security.CurrentAccount;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;
    private final CurrentAccount currentAccount;

    public UploadController(UploadService uploadService, CurrentAccount currentAccount) {
        this.uploadService = uploadService;
        this.currentAccount = currentAccount;
    }

    /**
     * Any authenticated account may upload - the resulting URL is just pasted
     * into whichever field the caller owns; tenant checks happen when that
     * field is saved.
     *
     * The account is passed through so the bytes are counted against its
     * storage quota. Nothing counted them before, and the rate limit alone
     * bounds only the speed an account can fill the bucket at, not how full it
     * gets.
     */
    @PostMapping("/images")
    public ApiResponse<UploadResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            // Optional: the browser makes a small copy before uploading, and an
            // older client or a direct API call simply will not send one.
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            HttpServletRequest request) {
        String key = uploadService.storeImage(file, thumbnail, currentAccount.get().accountId());

        // Object storage knows its own public address. Local disk does not -
        // those files are served by this application, so the right host is
        // whichever one this request arrived on, and only here knows that.
        String url = uploadService.publicUrl(key);
        if (url == null) {
            url = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
                    .replacePath("/uploads/" + key)
                    .replaceQuery(null)
                    .toUriString();
        }
        return ApiResponse.ok(new UploadResponse(url));
    }

    public record UploadResponse(String url) {
    }
}
