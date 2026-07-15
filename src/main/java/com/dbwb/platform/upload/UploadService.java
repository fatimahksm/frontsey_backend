package com.dbwb.platform.upload;

import com.dbwb.platform.common.exception.BusinessRuleViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stores owner-uploaded images (logos, cover photos, gallery, menu items,
 * services) on local disk and hands back the filename the caller needs to
 * build a public URL from. Replaces the earlier "paste an image URL"
 * workflow with a real upload.
 */
@Service
public class UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final UploadProperties properties;

    public UploadService(UploadProperties properties) {
        this.properties = properties;
    }

    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("No file was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessRuleViolationException("Only JPEG, PNG, WEBP, or GIF images are allowed.");
        }
        long maxBytes = properties.getMaxFileSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessRuleViolationException("Image must be smaller than " + properties.getMaxFileSizeMb() + "MB.");
        }

        try {
            Path directory = Path.of(properties.getDirectory());
            Files.createDirectories(directory);

            String filename = UUID.randomUUID() + EXTENSION_BY_CONTENT_TYPE.get(contentType);
            Path destination = directory.resolve(filename);
            file.transferTo(destination);
            return filename;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the uploaded image.", e);
        }
    }
}
