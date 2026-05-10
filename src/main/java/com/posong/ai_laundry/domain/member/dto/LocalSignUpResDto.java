package com.posong.ai_laundry.domain.member.dto;

import java.time.LocalDateTime;

public record LocalSignUpResDto(
		Long memberId,
		String loginId,
		String email,
		LocalDateTime createdAt
) {
}
