package com.posong.ai_laundry.domain.member.dto;

import java.time.LocalDateTime;

public record TokenReissueResDto(
		String grantType,
		String accessToken,
		LocalDateTime accessTokenExpiresAt,
		String refreshToken,
		LocalDateTime refreshTokenExpiresAt
) {
}
