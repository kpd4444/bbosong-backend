package com.posong.ai_laundry.global.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
		@NotBlank(message = "jwt.secret은 필수입니다.")
		@Size(min = 32, message = "jwt.secret은 32자 이상이어야 합니다.")
		String secret,

		@Min(value = 1, message = "jwt.accessTokenExpirationSeconds는 1 이상이어야 합니다.")
		long accessTokenExpirationSeconds,

		@Min(value = 1, message = "jwt.refreshTokenExpirationSeconds는 1 이상이어야 합니다.")
		long refreshTokenExpirationSeconds
) {
}
