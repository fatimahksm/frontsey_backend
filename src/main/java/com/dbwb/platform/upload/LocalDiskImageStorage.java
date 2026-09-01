package com.dbwb.platform.upload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Files on this container's own disk, served back by UploadWebConfig under
 * /uploads.
 *
 * The development default, and deliberately not a production one: the files
 * are lost when the container is replaced, and a second instance serves 404s
 * for everything the first one holds.
 */
@Component
@ConditionalOnProperty(name = "dbwb.uploads.storage", havingValue = "local", matchIfMissing = true)
public class LocalDiskImageStorage implements ImageStorage {

    private final UploadProperties properties;

    public LocalDiskImageStorage(UploadProperties properties) {
        this.properties = properties;
    }

    @Override
    public String store(byte[] content, String contentType, String extension) {
        try {
            Path directory = Path.of(properties.getDirectory());
            Files.createDirectories(directory);
            String key = UUID.randomUUID() + extension;
            Files.write(directory.resolve(key), content);
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store the uploaded image.", e);
        }
    }

    /**
     * Null on purpose: these are served by this application, so the correct
     * host is the one the request came in on, which only the controller knows.
     */
    @Override
    public String publicUrl(String key) {
        return null;
    }
}
