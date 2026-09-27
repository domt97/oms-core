package com.dotran.oms.core.cloud.s3;

import com.dotran.oms.core.cloud.config.AwsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
@Slf4j
public class S3Config {

    private static final String LOCALDEV = "localdev";

    @Autowired
    private Environment environment;

    @Autowired
    private AwsProperties awsProperties;

    @Bean
    public S3Client s3Client() {
        // LocalStack
        if (environment.matchesProfiles(LOCALDEV)) {
            log.info("Init s3Client() for localdev env");
            return S3Client.builder()
                    .endpointOverride(URI.create(awsProperties.getEndpoint()))
                    .region(Region.of(awsProperties.getRegion()))
                    .credentialsProvider(
                            StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create("test", "test")
                            )
                    )
                    .forcePathStyle(true)
                    .build();
        }

        // AWS
        var clientBuilder = S3Client.builder()
                .region(Region.of(awsProperties.getRegion()));

        // Optional endpoint override
        if (awsProperties.getEndpoint() != null && !awsProperties.getEndpoint().isEmpty()) {
            clientBuilder.endpointOverride(URI.create(awsProperties.getEndpoint()));
        }

        // Optional static credentials
        if (awsProperties.getAccessKey() != null
                && !awsProperties.getAccessKey().isEmpty()
                && awsProperties.getSecretKey() != null
                && !awsProperties.getSecretKey().isEmpty()) {

            clientBuilder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    awsProperties.getAccessKey(),
                                    awsProperties.getSecretKey()
                            )
                    )
            );
        }

        return clientBuilder.build();
    }
}
