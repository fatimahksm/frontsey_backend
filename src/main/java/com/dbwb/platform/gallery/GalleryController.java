package com.dbwb.platform.gallery;

import com.dbwb.platform.common.dto.ApiResponse;
import com.dbwb.platform.gallery.dto.GalleryImageRequest;
import com.dbwb.platform.gallery.dto.GalleryImageResponse;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/websites/{websiteId}/gallery")
public class GalleryController {

    private final GalleryService galleryService;
    private final CurrentAccount currentAccount;

    public GalleryController(GalleryService galleryService, CurrentAccount currentAccount) {
        this.galleryService = galleryService;
        this.currentAccount = currentAccount;
    }

    @GetMapping
    public ApiResponse<List<GalleryImageResponse>> list(@PathVariable UUID websiteId) {
        return ApiResponse.ok(galleryService.list(websiteId, currentAccount.get())
                .stream().map(GalleryImageResponse::from).toList());
    }

    @PostMapping
    public ApiResponse<GalleryImageResponse> add(@PathVariable UUID websiteId, @Valid @RequestBody GalleryImageRequest request) {
        var image = galleryService.add(websiteId, currentAccount.get(), request.imageUrl());
        return ApiResponse.ok(GalleryImageResponse.from(image), "Image added.");
    }

    @DeleteMapping("/{imageId}")
    public ApiResponse<Void> delete(@PathVariable UUID websiteId, @PathVariable UUID imageId) {
        galleryService.delete(websiteId, imageId, currentAccount.get());
        return ApiResponse.ok(null, "Image deleted.");
    }

    @PutMapping("/{imageId}/cover")
    public ApiResponse<Void> setCover(@PathVariable UUID websiteId, @PathVariable UUID imageId) {
        galleryService.setCover(websiteId, imageId, currentAccount.get());
        return ApiResponse.ok(null, "Cover image updated.");
    }

    @PutMapping("/reorder")
    public ApiResponse<Void> reorder(@PathVariable UUID websiteId, @RequestBody List<UUID> imageIds) {
        galleryService.reorder(websiteId, currentAccount.get(), imageIds);
        return ApiResponse.ok(null, "Gallery reordered.");
    }
}
