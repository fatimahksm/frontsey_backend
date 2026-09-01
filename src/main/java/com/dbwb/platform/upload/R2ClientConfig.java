package com.dbwb.platform.upload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;

/** The S3 client, pointed at Cloudflare R2. Only built when R2 is the chosen storage. */
@Configuration
@ConditionalOnProperty(name = "dbwb.uploads.storage", havingValue = "r2")
public class R2ClientConfig {

    @Bean
    public S3Client r2Client(UploadProperties properties) {
        UploadProperties.R2 config = properties.getR2();
        if (!config.isConfigured()) {
            // Failing at startup rather than on the first upload. A missing key
            // discovered when an owner tries to add a photo is a support
            // ticket; discovered at boot it is a deploy that did not finish.
            throw new IllegalStateException(
                    "dbwb.uploads.storage is r2 but its credentials are incomplete. "
                            + "Set R2_ACCOUNT_ID, R2_BUCKET, R2_ACCESS_KEY_ID and R2_SECRET_ACCESS_KEY.");
        }

        return S3Client.builder()
                // R2 has no regions, but the SDK insists on one; "auto" is what
                // Cloudflare's own documentation uses.
                .region(Region.of("auto"))
                .endpointOverride(URI.create(config.endpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.getAccessKeyId(), config.getSecretAccessKey())))
                // Bucket in the path rather than the hostname. R2's endpoint is
                // per account, not per bucket, so virtual-host addressing - the
                // SDK's default - would build a hostname that does not resolve.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }
}
