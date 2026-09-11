package com.dbwb.platform.upload.entity;

import com.dbwb.platform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * One stored object and who is paying for it.
 *
 * Written for the original and again for its thumbnail, because each is an
 * object in the bucket taking its own bytes. The account is the one that made
 * the request rather than the website the image ends up on: an image is
 * uploaded before anything is saved, so at that moment there is no website to
 * attribute it to, and the quota is a limit on the owner in any case.
 */
@Entity
@Table(name = "uploaded_images")
public class UploadedImage extends BaseEntity {

    @Column(nullable = false)
    private UUID accountId;

    /** What ImageStorage returned - the key, not the public URL, which depends on where it is served from. */
    @Column(nullable = false, unique = true)
    private String storageKey;

    @Column(nullable = false)
    private long byteSize;

    @Column(nullable = false)
    private String contentType;

    protected UploadedImage() {
    }

    public UploadedImage(UUID accountId, String storageKey, long byteSize, String contentType) {
        this.accountId = accountId;
        this.storageKey = storageKey;
        this.byteSize = byteSize;
        this.contentType = contentType;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public long getByteSize() {
        return byteSize;
    }

    public String getContentType() {
        return contentType;
    }
}
