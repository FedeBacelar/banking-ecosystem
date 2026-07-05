package com.fedebacelar.bank.document.infrastructure.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfig {

    @Bean
    S3Client s3Client(
            @Value("${document.storage.endpoint}") String endpoint,
            @Value("${document.storage.access-key}") String accessKey,
            @Value("${document.storage.secret-key}") String secretKey,
            @Value("${document.storage.region:us-east-1}") String region,
            @Value("${document.storage.path-style-access:true}") boolean pathStyleAccess
    ) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build())
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
    }
}

