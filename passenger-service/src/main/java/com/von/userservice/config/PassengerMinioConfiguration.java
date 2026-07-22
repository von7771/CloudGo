package com.von.userservice.config;

import com.von.common.storage.MinioStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true", matchIfMissing = true)
public class PassengerMinioConfiguration {

    @Bean
    public MinioStorageService minioStorageService(
            @Value("${minio.endpoint:http://minio:9000}") String endpoint,
            @Value("${minio.access-key:CHANGE_ME_MINIO_ACCESS_KEY}") String accessKey,
            @Value("${minio.secret-key:CHANGE_ME_MINIO_SECRET_KEY}") String secretKey,
            @Value("${minio.bucket:carpool}") String bucket) {
        return new MinioStorageService(endpoint, accessKey, secretKey, bucket);
    }
}
