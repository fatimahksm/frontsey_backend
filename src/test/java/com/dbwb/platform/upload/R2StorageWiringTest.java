package com.dbwb.platform.upload;

import com.adobe.testing.s3mock.junit5.S3MockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the whole application with dbwb.uploads.storage=r2 and checks the
 * container ends up wired the way production would be.
 *
 * The unit test proves R2ImageStorage talks to S3 correctly. This proves the
 * application actually picks it - that the @ConditionalOnProperty switches
 * land the right way round, and that /uploads stops being served by us when
 * Cloudflare is serving it instead. Those are the parts that would silently
 * fall back to local disk in production and look fine until the first restart
 * lost every photograph.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "dbwb.uploads.storage=r2",
        "dbwb.uploads.r2.bucket=frontsey-wiring",
        "dbwb.uploads.r2.public-base-url=https://images.example.com",
        // Dummy, but present: R2ClientConfig refuses to build a client without
        // credentials, and it is right to - that check is what turns a missing
        // secret into a deploy that stops rather than an upload that fails
        // later. The real client is built and then never used; @Primary sends
        // the injection to the local one.
        "dbwb.uploads.r2.account-id=test-account",
        "dbwb.uploads.r2.access-key-id=test-key",
        "dbwb.uploads.r2.secret-access-key=test-secret",
})
@Import(R2StorageWiringTest.LocalS3.class)
class R2StorageWiringTest {

    @RegisterExtension
    static final S3MockExtension S3_MOCK = S3MockExtension.builder().silent().withSecureConnection(false).build();

    /**
     * Stands in for the real client so the context does not try to reach
     * Cloudflare.
     *
     * @Primary rather than relying on @ConditionalOnMissingBean in
     * R2ClientConfig: that annotation only behaves predictably on
     * auto-configuration, and on an ordinary @Configuration it is evaluated
     * before a @TestConfiguration is registered - so both clients were built
     * and the injection point became ambiguous. It was giving false assurance,
     * and is gone.
     */
    @TestConfiguration
    static class LocalS3 {
        @Bean
        @Primary
        S3Client s3Client() {
            S3Client client = S3_MOCK.createS3ClientV2();
            if (client.listBuckets().buckets().stream().noneMatch(b -> "frontsey-wiring".equals(b.name()))) {
                client.createBucket(CreateBucketRequest.builder().bucket("frontsey-wiring").build());
            }
            return client;
        }
    }

    @Autowired
    private ImageStorage storage;
    @Autowired
    private ApplicationContext context;

    @Test
    void usesR2RatherThanTheContainersDisk() {
        assertThat(storage).isInstanceOf(R2ImageStorage.class);
    }

    @Test
    void stopsServingUploadsItselfWhenCloudflareIsServingThem() {
        // Registering /uploads/** under R2 would advertise a path holding
        // nothing, so the handler is conditional on local storage.
        assertThat(context.getBeanNamesForType(UploadWebConfig.class)).isEmpty();
    }

    @Test
    void aStoredImageGetsAPublicUrlOnTheConfiguredDomain() {
        String key = storage.store("bytes".getBytes(), "image/png", ".png");

        assertThat(storage.publicUrl(key)).isEqualTo("https://images.example.com/" + key);
    }
}
