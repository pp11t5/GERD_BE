package com.gerd.global.config

import com.gerd.global.config.properties.R2Properties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import java.net.URI

@Configuration
class R2Config(private val r2Properties: R2Properties) {

    // R2는 S3 호환 API — endpointOverride로 Cloudflare 엔드포인트 지정
    @Bean
    fun r2Client(): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(r2Properties.endpoint))
            .region(Region.of("auto"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                        r2Properties.credentials.accessKey,
                        r2Properties.credentials.secretKey,
                    ),
                ),
            )
            .build()
}
