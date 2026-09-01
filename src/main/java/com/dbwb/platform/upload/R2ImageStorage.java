package com.dbwb.platform.upload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

/**
 * Cloudflare R2, through the S3 API.
 *
 * Chosen over the local disk for the two reasons that matter: the files
 * outlive the container, and every instance sees the same ones. It also
 * answers the CDN question at the same time - a public R2 bucket is served
 * from Cloudflare's own network, so there is no separate CDN to put in front.
 *
 * Objects are not made public here. R2 ignores S3 ACLs; a bucket is made
 * readable in the Cloudflare dashboard, and its r2.dev address or custom
 * domain becomes dbwb.uploads.r2.public-base-url. Sending an ACL would be
 * noise at best and an error at worst.
 */
@Component
@ConditionalOnProperty(name = "dbwb.uploads.storage", havingValue = "r2")
public class R2ImageStorage implements ImageStorage {

    /**
     * A year, public, immutable - the same as the local handler sends, and for
     * the same reason: the key is a fresh UUID on every upload and editing an
     * image produces a new object, so nothing at a given key ever changes.
     * Set at upload time because there is no server of ours in the path to add
     * it later; Cloudflare serves the object exactly as stored.
     */
    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final S3Client client;
    private final UploadProperties.R2 config;

    public R2ImageStorage(S3Client client, UploadProperties properties) {
        this.client = client;
        this.config = properties.getR2();
    }

    @Override
    public String store(byte[] content, String contentType, String extension) {
        String key = UUID.randomUUID() + extension;
        client.putObject(
                PutObjectRequest.builder()
                        .bucket(config.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .cacheControl(CACHE_CONTROL)
                        .build(),
                RequestBody.fromBytes(content));
        return key;
    }

    @Override
    public String publicUrl(String key) {
        String base = config.getPublicBaseUrl();
        if (base == null || base.isBlank()) {
            // Storing images nobody can load is worse than refusing the upload:
            // the owner would see it save and then find a broken picture on
            // their live site with nothing to explain it.
            throw new IllegalStateException(
                    "dbwb.uploads.r2.public-base-url is not set, so there is no address to serve "
                            + "uploaded images from. Set it to the bucket's r2.dev address or its custom domain.");
        }
        return base.replaceAll("/+$", "") + "/" + key;
    }
}
