package com.dbwb.platform.upload;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/** Serves uploaded images back out at /uploads/** - see SecurityConfig for why that path is public. */
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    private final UploadProperties properties;

    public UploadWebConfig(UploadProperties properties) {
        this.properties = properties;
    }

    /**
     * Uploaded files are immutable: the name is a fresh UUID on every upload,
     * and editing an image produces a new file rather than replacing one. So
     * they can be cached hard and for a long time, which is the difference
     * between a returning visitor re-downloading a menu's photographs and not.
     *
     * Served with no cache headers at all before this, so every visit re-fetched
     * every image.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path directory = Path.of(properties.getDirectory()).toAbsolutePath();
        try {
            // Created here, not only on first upload. Without it a fresh
            // deployment answers 500 to every image request until somebody
            // happens to upload one, because the handler's location does not
            // resolve to a directory that exists.
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create the upload directory: " + directory, e);
        }
        String location = directory.toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
