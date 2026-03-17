package com.revplay.musicplatform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            awsProperties.getAccessKey(),
            awsProperties.getSecretKey()
        );

        return S3Client.builder()
            .region(Region.of(awsProperties.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build();
    }
}
