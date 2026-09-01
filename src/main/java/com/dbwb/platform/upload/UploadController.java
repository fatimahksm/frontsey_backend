package com.dbwb.platform.upload;

import com.dbwb.platform.common.dto.ApiResponse;
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

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /** Any authenticated account may upload - the resulting URL is just pasted into whichever field the caller owns; tenant checks happen when that field is saved. */
    @PostMapping("/images")
    public ApiResponse<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        String key = uploadService.storeImage(file);

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
