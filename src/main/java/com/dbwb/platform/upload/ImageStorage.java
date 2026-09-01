package com.dbwb.platform.upload;

/**
 * Where an uploaded image ends up.
 *
 * Two implementations, chosen by dbwb.uploads.storage. Local disk is for
 * development: the files do not survive a restart and two instances cannot see
 * each other's, which is fine on one throwaway container and disqualifying in
 * production. R2 is the real one.
 *
 * Validation is not in here. UploadService decides what may be stored - size,
 * and what the bytes actually are - and this only puts approved bytes
 * somewhere and says where they landed.
 */
public interface ImageStorage {

    /**
     * Stores the image under a fresh, unguessable key and returns that key.
     *
     * @param content     the bytes, already validated
     * @param contentType the type the bytes were identified as, not the one the client claimed
     * @param extension   the file extension for that type, including the dot
     */
    String store(byte[] content, String contentType, String extension);

    /**
     * The absolute URL a visitor's browser should load this key from, or null
     * when the caller should build one from the incoming request instead.
     *
     * Null is the local-disk case with no base URL configured: the files are
     * served by this same application under /uploads, so the right host is
     * whichever one the request arrived on, and only the caller knows that.
     */
    String publicUrl(String key);
}
