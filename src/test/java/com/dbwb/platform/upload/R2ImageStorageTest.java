package com.dbwb.platform.upload;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The R2 path, run against a real S3 server rather than a mocked client.
 *
 * S3MockExtension starts one in this JVM, so the actual AWS SDK does actual
 * HTTP with actual signing - which is where the things that go wrong live.
 * Mocking S3Client would have proved only that the code calls the method it
 * obviously calls.
 *
 * R2 is not S3, but it is the S3 API, and everything asserted here is that
 * API: the object lands under the key, the bytes survive, and the metadata a
 * browser needs is on it.
 */
class R2ImageStorageTest {

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent().withSecureConnection(false).build();

    private static final String BUCKET = "frontsey-images";

    private S3Client client;
    private UploadProperties properties;
    private R2ImageStorage storage;

    @BeforeEach
    void setUp() {
        client = S3_MOCK.createS3ClientV2();
        // The mock server is shared across the class, so the bucket outlives
        // any one test - create it only if it is not already there.
        if (client.listBuckets().buckets().stream().noneMatch(b -> BUCKET.equals(b.name()))) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }

        properties = new UploadProperties();
        properties.getR2().setBucket(BUCKET);
        properties.getR2().setPublicBaseUrl("https://images.example.com");
        storage = new R2ImageStorage(client, properties);
    }

    @Test
    void putsTheImageInTheBucketAndHandsBackAKeyThatFindsIt() {
        byte[] content = "not really a jpeg, but these exact bytes".getBytes(StandardCharsets.UTF_8);

        String key = storage.store(content, "image/jpeg", ".jpg");

        assertThat(key).endsWith(".jpg");
        try (ResponseInputStream<GetObjectResponse> stored =
                     client.getObject(GetObjectRequest.builder().bucket(BUCKET).key(key).build())) {
            assertThat(stored.readAllBytes()).isEqualTo(content);
            assertThat(stored.response().contentType()).isEqualTo("image/jpeg");
        } catch (Exception e) {
            throw new AssertionError("stored object could not be read back", e);
        }
    }

    @Test
    void marksTheObjectCacheableForAYear() {
        // Nothing of ours is in the path when Cloudflare serves this, so the
        // header has to be on the object itself or it is never sent at all.
        String key = storage.store("bytes".getBytes(StandardCharsets.UTF_8), "image/png", ".png");

        GetObjectResponse response = client.getObject(
                GetObjectRequest.builder().bucket(BUCKET).key(key).build()).response();

        assertThat(response.cacheControl()).isEqualTo("public, max-age=31536000, immutable");
    }

    @Test
    void givesEveryUploadItsOwnKey() {
        // Two owners uploading logo.png must not overwrite each other, so the
        // key is a fresh UUID rather than anything from the file.
        byte[] content = "x".getBytes(StandardCharsets.UTF_8);

        assertThat(storage.store(content, "image/png", ".png"))
                .isNotEqualTo(storage.store(content, "image/png", ".png"));
    }

    @Test
    void buildsThePublicUrlFromTheConfiguredAddress() {
        assertThat(storage.publicUrl("abc.jpg")).isEqualTo("https://images.example.com/abc.jpg");
    }

    @Test
    void toleratesATrailingSlashOnTheConfiguredAddress() {
        // Pasted from the Cloudflare dashboard, this often has one.
        properties.getR2().setPublicBaseUrl("https://images.example.com/");

        assertThat(new R2ImageStorage(client, properties).publicUrl("abc.jpg"))
                .isEqualTo("https://images.example.com/abc.jpg");
    }

    @Test
    void refusesToInventAnAddressWhenNoneIsConfigured() {
        // Storing images nobody can load is worse than failing: the owner sees
        // it save, then finds a broken picture on their live site.
        properties.getR2().setPublicBaseUrl("");

        assertThatThrownBy(() -> new R2ImageStorage(client, properties).publicUrl("abc.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("public-base-url");
    }

    @Test
    void theWholeUploadPathWorksEndToEndOverS3() {
        // UploadService validates, R2ImageStorage stores - the seam between
        // them is what this exercises.
        UploadService uploadService = new UploadService(properties, storage);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        String key = uploadService.storeImage(
                new org.springframework.mock.web.MockMultipartFile("file", "logo.png", "image/png", png));

        assertThat(uploadService.publicUrl(key)).isEqualTo("https://images.example.com/" + key);
        assertThat(client.getObject(GetObjectRequest.builder().bucket(BUCKET).key(key).build())
                .response().contentType()).isEqualTo("image/png");
    }
}
