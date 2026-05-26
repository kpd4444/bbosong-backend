package com.posong.ai_laundry.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageUrlResolver {

	private final AwsStorageProperties awsStorageProperties;

	public String resolve(String imageKey) {
		if (imageKey == null || imageKey.isBlank()) {
			return null;
		}

		String domain = awsStorageProperties.cloudfront() == null ? null : awsStorageProperties.cloudfront().domain();
		if (domain == null || domain.isBlank()) {
			return imageKey;
		}

		return domain.replaceAll("/+$", "") + "/" + imageKey.replaceAll("^/+", "");
	}
}
