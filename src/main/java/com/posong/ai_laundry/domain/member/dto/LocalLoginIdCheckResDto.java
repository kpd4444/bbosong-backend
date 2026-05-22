package com.posong.ai_laundry.domain.member.dto;

public record LocalLoginIdCheckResDto(
		String loginId,
		boolean available,
		boolean duplicated
) {
	public static LocalLoginIdCheckResDto of(String loginId, boolean duplicated) {
		return new LocalLoginIdCheckResDto(loginId, !duplicated, duplicated);
	}
}
