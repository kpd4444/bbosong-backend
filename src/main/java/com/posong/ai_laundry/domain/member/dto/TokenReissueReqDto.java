package com.posong.ai_laundry.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TokenReissueReqDto(
		@Schema(description = "재발급에 사용할 refresh token", example = "eyJhbGciOiJIUzI1NiJ9...")
		@NotBlank(message = "리프레시 토큰은 필수입니다.")
		String refreshToken
) {
}
