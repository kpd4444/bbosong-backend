package com.posong.ai_laundry.domain.member.dto;

import java.time.LocalDate;

public record MemberProfileResDto(
		Long memberId,
		String email,
		String nickname,
		LocalDate birth
) {
}
