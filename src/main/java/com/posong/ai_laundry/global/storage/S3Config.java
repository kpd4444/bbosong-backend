package com.posong.ai_laundry.global.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

	@Bean
	public S3Client s3Client(AwsStorageProperties awsStorageProperties) {
		String region = awsStorageProperties.region() == null || awsStorageProperties.region().isBlank()
				? "ap-northeast-2"
				: awsStorageProperties.region();
		return S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(DefaultCredentialsProvider.create())
				.build();
	}
}
