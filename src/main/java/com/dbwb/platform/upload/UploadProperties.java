package com.dbwb.platform.upload;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Image upload storage config (dbwb.uploads.*) - local disk for development, Cloudflare R2 in production. */
@Configuration
@ConfigurationProperties(prefix = "dbwb.uploads")
public class UploadProperties {

    public enum Storage {
        /**
         * Files on the container's own disk. Fine for development and a single
         * throwaway instance; they do not survive a restart and two instances
         * cannot see each other's, so this is not a production choice.
         */
        LOCAL,
        /** Cloudflare R2, via the S3 API. Survives restarts, shared across instances, served by Cloudflare's network. */
        R2
    }

    private Storage storage = Storage.LOCAL;

    /** Where LOCAL writes. Ignored under R2. */
    private String directory = "uploads";

    private int maxFileSizeMb = 5;

    private final R2 r2 = new R2();

    /**
     * Cloudflare R2. It speaks the S3 API, so the S3 SDK is pointed at an R2
     * endpoint rather than there being an R2-specific client.
     */
    public static class R2 {
        /** From the Cloudflare dashboard; the endpoint is derived from it. */
        private String accountId = "";
        private String bucket = "";
        private String accessKeyId = "";
        private String secretAccessKey = "";

        /**
         * Where the public site reads the images from - the bucket's r2.dev
         * address or, better, a custom domain on it.
         *
         * Separate from the API endpoint on purpose: uploads go to
         * {accountId}.r2.cloudflarestorage.com with credentials, while visitors
         * read from a public hostname with none. Objects are made readable by
         * making the bucket public in Cloudflare, not per object - R2 ignores
         * S3 ACLs, so there is nothing to set at upload time.
         */
        private String publicBaseUrl = "";

        public String endpoint() {
            return "https://" + accountId + ".r2.cloudflarestorage.com";
        }

        public boolean isConfigured() {
            return !accountId.isBlank() && !bucket.isBlank()
                    && !accessKeyId.isBlank() && !secretAccessKey.isBlank();
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKeyId() {
            return accessKeyId;
        }

        public void setAccessKeyId(String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public String getSecretAccessKey() {
            return secretAccessKey;
        }

        public void setSecretAccessKey(String secretAccessKey) {
            this.secretAccessKey = secretAccessKey;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public void setMaxFileSizeMb(int maxFileSizeMb) {
        this.maxFileSizeMb = maxFileSizeMb;
    }

    public R2 getR2() {
        return r2;
    }
}
