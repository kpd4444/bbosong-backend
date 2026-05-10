package com.posong.ai_laundry.global.security;

import java.time.LocalDateTime;

public record TokenPair(
		String grantType,
		String accessToken,
		LocalDateTime accessTokenExpiresAt,
		String refreshToken,
		LocalDateTime refreshTokenExpiresAt
) {
}
