package com.posong.ai_laundry.global.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsStorageProperties(
		String region,
		S3 s3,
		CloudFront cloudfront
) {
	public record S3(String bucket) {
	}

	public record CloudFront(String domain) {
	}
}
