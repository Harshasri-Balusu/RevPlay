package com.revplay.musicplatform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "s3")
public class S3Config {

    @Bean
    public S3Client s3Client(AwsProperties awsProperties) {
        var builder = S3Client.builder().region(Region.of(awsProperties.getRegion()));
        if (StringUtils.hasText(awsProperties.getAccessKey())
                && StringUtils.hasText(awsProperties.getSecretKey())) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsProperties.getAccessKey(), awsProperties.getSecretKey())));
        } else {
            // EC2 instance profile / IAM role, env vars, etc. (avoids "Access key ID cannot be blank")
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }
}
